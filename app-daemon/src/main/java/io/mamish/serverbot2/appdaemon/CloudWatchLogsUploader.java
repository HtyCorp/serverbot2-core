package io.mamish.serverbot2.appdaemon;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CloudWatchLogsUploader {

    // Chosen because CloudWatch Logs group names don't allow ':' characters.
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss.SSSVV")
            .withZone(ZoneId.systemDefault());
    private static final CloudWatchLogsClient logsClient = CloudWatchLogsClient.create();

    // Cannot exceed CloudWatch Logs API limit of 10000
    private static final int MAX_BUFFERED_MESSAGES = 2048;
    private static final int FLUSH_THRESHOLD_MIN_CAPACITY = 1024;
    private static final int FLUSH_THRESHOLD_MAX_INTERVAL_SECONDS = 3;

    private final BufferedReader streamReader;
    private final BlockingQueue<InputLogEvent> outgoingEntryQueue = new ArrayBlockingQueue<>(MAX_BUFFERED_MESSAGES);

    private final String logGroupName;
    private final String logStreamName;
    private String nextUploadToken;
    private Instant lastUploadTime = Instant.now();

    public CloudWatchLogsUploader(InputStream inputStream, String appName, Instant when, String outputType) {
        this.streamReader = new BufferedReader(new InputStreamReader(inputStream));

        this.logGroupName = IDUtils.slash(AppInstanceConfig.APP_LOGS_GROUP_PREFIX, appName);
        this.logStreamName = IDUtils.slash(timeFormatter.format(when), outputType);
        logsClient.createLogStream(r -> r.logGroupName(logGroupName).logStreamName(logStreamName));

        new Thread(this::streamReadLoop).start();
        new Thread(this::streamUploadLoop).start();
    }

    private void streamReadLoop() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        try {
            String logLine;
            while ((logLine = streamReader.readLine()) != null) {
                InputLogEvent event = InputLogEvent.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .message(logLine)
                        .build();
                outgoingEntryQueue.put(event);
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
        try {
            while (true) {
                synchronized (this) {
                    while (outgoingEntryQueue.remainingCapacity() > FLUSH_THRESHOLD_MIN_CAPACITY
                            && lastUploadTime.until(Instant.now(), ChronoUnit.SECONDS) < FLUSH_THRESHOLD_MAX_INTERVAL_SECONDS) {
                        wait();
                    }
                }
                lastUploadTime = Instant.now();
                final int approxAvailable = outgoingEntryQueue.size() - outgoingEntryQueue.remainingCapacity();
                List<InputLogEvent> eventsBatch = new ArrayList<>(approxAvailable);
                outgoingEntryQueue.drainTo(eventsBatch, approxAvailable);
                nextUploadToken = logsClient.putLogEvents(r ->
                        r.logGroupName(logGroupName)
                        .logStreamName(logStreamName)
                        .sequenceToken(nextUploadToken)
                        .logEvents(eventsBatch)
                ).nextSequenceToken();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
