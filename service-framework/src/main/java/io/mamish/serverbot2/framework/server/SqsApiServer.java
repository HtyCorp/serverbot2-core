package io.mamish.serverbot2.framework.server;

import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SqsApiServer<ModelType> {

    private static final String THREAD_NAME = "SqsApiRequestReceiverThread";

    private final JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(getHandlerInstance(),getModelClass());
    private final SqsClient sqsClient = SqsClient.create();
    private final SqsAsyncClient sqsAsyncClient = SqsAsyncClient.create();
    private final String receiveQueueUrl = sqsClient.getQueueUrl(r -> r.queueName(getReceiverQueueName())).queueUrl();
    private final Logger logger = Logger.getLogger("SqsStandardApiHandler");

    protected abstract Class<ModelType> getModelClass();
    protected abstract ModelType getHandlerInstance();
    protected abstract String getReceiverQueueName();

    public SqsApiServer() {
        // Don't run as daemon: this is intended as a forever-running server thread.
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
            while(true) {
                List<Message> messages = sqsClient.receiveMessage(r -> r.queueUrl(receiveQueueUrl)
                        .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS)).messages();
                messages.forEach(m -> {
                    // Deletion is synchronous to make exception handling simpler, though it could be made async.
                    sqsClient.deleteMessage(r -> r.queueUrl(receiveQueueUrl).receiptHandle(m.receiptHandle()));
                    String replyQueueUrl = m.messageAttributes().get(ApiConfig.JSON_REQUEST_QUEUE_KEY).stringValue();
                    String requestId = m.messageAttributes().get(ApiConfig.JSON_REQUEST_ID_KEY).stringValue();

                    String responseString = jsonApiHandler.handleRequest(m.body());

                    Map<String,MessageAttributeValue> sqsAttrMap = Map.of(
                            ApiConfig.JSON_REQUEST_ID_KEY, stringAttribute(requestId)
                            // No point putting the queue URL attribute back in.
                    );
                    // Send reply async: don't need to wait for answer, and don't care if it fails.
                    try {
                        sqsAsyncClient.sendMessage(r -> r.queueUrl(replyQueueUrl)
                                .messageAttributes(sqsAttrMap)
                                .messageBody(responseString));
                    } catch (Exception e) {
                        // Reply queue send might fail outside the control of this service, so don't make it fatal.
                        logger.log(Level.WARNING, "Unable to send SQS response message", e);
                    }
                });
            }
        } catch (SdkException e) {
            // Can't (or shouldn't) recover from operations failures on our own queue. Let the thread die instead.
            logger.log(Level.SEVERE, "Fatal error in SQS receive loop", e);
        }
    }

    private static MessageAttributeValue stringAttribute(String s) {
        return MessageAttributeValue.builder().stringValue(s).build();
    }

}
