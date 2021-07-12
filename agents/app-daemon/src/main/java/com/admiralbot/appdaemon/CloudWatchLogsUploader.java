package com.admiralbot.appdaemon;

import com.admiralbot.sharedconfig.AppInstanceConfig;
import com.admiralbot.sharedutil.Joiner;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CloudWatchLogsUploader {

    // Chosen because CloudWatch Logs group names don't allow ':' characters.
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss.SSSz")
            .withZone(ZoneId.systemDefault());
    private static final CloudWatchLogsClient logsClient = CloudWatchLogsClient.create();

    // Cannot exceed CloudWatch Logs API limit of 10000
    private static final int MAX_BUFFERED_MESSAGES = 2048;
    private static final int FLUSH_THRESHOLD_MIN_CAPACITY = 1024;
    private static final int FLUSH_THRESHOLD_MAX_INTERVAL_MILLIS = 3000;

    private final BufferedReader streamReader;
    private final BlockingQueue<InputLogEvent> outgoingEntryQueue = new ArrayBlockingQueue<>(MAX_BUFFERED_MESSAGES);

    private final String logGroupName;
    private final String logStreamName;

    private final Logger logger = LoggerFactory.getLogger(CloudWatchLogsUploader.class);

    public CloudWatchLogsUploader(InputStream inputStream, String appName, Instant when, String outputType) {
        this.streamReader = new BufferedReader(new InputStreamReader(inputStream));

        this.logGroupName = Joiner.slash(AppInstanceConfig.APP_LOGS_GROUP_PREFIX, appName);
        this.logStreamName = Joiner.slash(timeFormatter.format(when), outputType);

        logger.debug("Creating log group '" + logGroupName + "' with log stream '" + logStreamName + "'");

        try {
            logsClient.createLogGroup(r -> r.logGroupName(logGroupName));
            logger.debug("Log group created for the first time");
        } catch (SdkException e) {
            logger.debug("Couldn't create log group. It probably already exists. SDK message: " + e.getMessage());
        }
        logsClient.createLogStream(r -> r.logGroupName(logGroupName).logStreamName(logStreamName));

        new Thread(this::streamReadLoop).start();
        new Thread(this::streamUploadLoop).start();
    }

    private void streamReadLoop() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        try {
            String logLine;
            while ((logLine = streamReader.readLine()) != null) {

                // API error if InputLogEvent.message is empty - just make it a space if so
                if (logLine.isEmpty()) {
                    logLine = " ";
                }

                logger.trace("Got log line: " + logLine);
                InputLogEvent event = InputLogEvent.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .message(logLine)
                        .build();
                outgoingEntryQueue.put(event);
                logger.trace("Remaining capacity is " + outgoingEntryQueue.remainingCapacity());

                if (outgoingEntryQueue.remainingCapacity() <= FLUSH_THRESHOLD_MIN_CAPACITY) {
                    synchronized (this) {
                        notifyAll();
                    }
                }

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void streamUploadLoop() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        String nextSequenceToken = null; // No sequence token specified for first batch of events uploaded
        try {
            while (true) {
                synchronized (this) {
                    logger.trace("Upload loop entering wait");
                    wait(FLUSH_THRESHOLD_MAX_INTERVAL_MILLIS);
                }

                final int numMessages = outgoingEntryQueue.size();
                if (numMessages == 0) {
                    logger.trace("No lines to upload");
                    continue;
                }

                logger.trace("Have " + numMessages + " lines to upload");
                List<InputLogEvent> eventsBatch = new ArrayList<>(numMessages);
                outgoingEntryQueue.drainTo(eventsBatch, numMessages);

                String finalNextSequenceToken = nextSequenceToken;
                nextSequenceToken = logsClient.putLogEvents(r ->
                        r.logGroupName(logGroupName)
                        .logStreamName(logStreamName)
                        .sequenceToken(finalNextSequenceToken)
                        .logEvents(eventsBatch)
                ).nextSequenceToken();
                logger.trace("Finished upload and got next sequence token " + nextSequenceToken);
            }
            // TODO: Spurious interrupts aren't impossible. Should have this resume logging if that occurs.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
