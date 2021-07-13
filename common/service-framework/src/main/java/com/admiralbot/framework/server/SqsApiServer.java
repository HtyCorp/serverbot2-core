package com.admiralbot.framework.server;

import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.XrayUtils;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

public abstract class SqsApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private static final String THREAD_NAME = "SqsApiRequestReceiverThread";

    private final SqsClient sqsClient = SqsClient.create();
    private final String receiveQueueName;

    private final Logger logger = LoggerFactory.getLogger(SqsApiServer.class);
    private final Gson gson = new Gson();

    @Override
    protected boolean requiresEndpointInfo() {
        return false;
    }

    public SqsApiServer(String receiveQueueName) {
        super.initialise();
        // Don't run as daemon: this is intended as a forever-running server thread.
        this.receiveQueueName = receiveQueueName;
        new Thread(this::runReceiveLoop, THREAD_NAME).start();
    }

    /*
     * Note on exception handling:
     *
     * Service is responsible for making operations on its own SQS queues work. If they fail and we blindly retry, it
     * could result in runaway SQS API calls and a larger bill than expected from the normal one-call-per-20-seconds
     * operation.
     *
     * Hence, failed operations on our queue should be fatal, while operations on reply queues are logged and ignored.
     */
    private void runReceiveLoop() {
        try {
            // The initialisation and polling SQS calls aren't of interest performance-wise but will throw an error if
            // used outside a trace due to missing Xray context. This lines sets the Xray recorder to ignore these.
            // This is only required because automatic client instrumentation is enabled (and can't be selectively
            // disabled per-client like I hoped...).
            XrayUtils.setIgnoreMissingContext();

            final String receiveQueueUrl = sqsClient.getQueueUrl(r -> r.queueName(receiveQueueName)).queueUrl();

            final List<String> receiveAttributeNames = List.of(
                    ApiConfig.JSON_REQUEST_ID_KEY,
                    ApiConfig.JSON_REQUEST_QUEUE_KEY
            );

            while(true) {
                logger.trace("Polling messages");
                ReceiveMessageResponse response = sqsClient.receiveMessage(r ->
                        r.queueUrl(receiveQueueUrl)
                        .messageAttributeNames(receiveAttributeNames)
                        .attributeNames(QueueAttributeName.ALL)
                        .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS)
                );
                if (!response.hasMessages()) {
                    logger.trace("No messages received");
                } else {
                    response.messages().forEach(message -> {

                        // Propagate Xray trace information if available in message attributes
                        String traceHeader = extractTraceHeaderIfAvailable(message);
                        XrayUtils.beginSegment("HandleRequest", traceHeader);

                        try {

                            if (!message.hasAttributes()) {
                                logger.warn("Received message does not have any attributes.");
                                return;
                            }

                            String replyQueueUrl;
                            String requestId;
                            try {
                                replyQueueUrl = message.messageAttributes().get(ApiConfig.JSON_REQUEST_QUEUE_KEY).stringValue();
                                requestId = message.messageAttributes().get(ApiConfig.JSON_REQUEST_ID_KEY).stringValue();
                            } catch (NullPointerException e) {
                                logger.warn("Message is missing required attributes. Attribute map: "
                                        + gson.toJson(message.messageAttributes()));
                                return;
                            }

                            LogUtils.infoDump(logger, "Message attributes:", message.messageAttributes());
                            logger.info("Request payload:\n" + message.body());

                            sqsClient.deleteMessage(r -> r.queueUrl(receiveQueueUrl).receiptHandle(message.receiptHandle()));

                            XrayUtils.beginSubsegment("DispatchRequest");
                            String responseString = getRequestDispatcher().handleRequest(message.body());
                            XrayUtils.endSubsegment();

                            logger.info("Response payload:\n" + responseString);

                            Map<String,MessageAttributeValue> sqsAttrMap = Map.of(
                                    ApiConfig.JSON_REQUEST_ID_KEY, stringAttribute(requestId)
                                    // No point putting the queue URL attribute back in.
                            );

                            XrayUtils.beginSubsegment("SendReply");
                            try {
                                sqsClient.sendMessage(r -> r.queueUrl(replyQueueUrl)
                                        .messageAttributes(sqsAttrMap)
                                        .messageBody(responseString));
                            } catch (Exception e) {
                                // Reply queue send might fail outside the control of this service, so don't make it fatal.
                                logger.warn("Unable to send SQS response message", e);
                                XrayUtils.addSubsegmentException(e);
                            } finally {
                                XrayUtils.endSubsegment();
                            }

                        } finally {
                            XrayUtils.endSegment();
                        }
                    });
                }
            }
        } catch (SdkException e) {
            // Can't (or shouldn't) recover from operations failures on our own queue. Let the thread die instead.
            logger.warn("Fatal error in SQS receive loop", e);
        }
    }

    private String extractTraceHeaderIfAvailable(Message message) {
        if (message.hasAttributes()) {
            return message.attributes().get(MessageSystemAttributeName.AWS_TRACE_HEADER);
        }
        return null;
    }

    private static MessageAttributeValue stringAttribute(String s) {
        return MessageAttributeValue.builder().stringValue(s).dataType("String").build();
    }

}
