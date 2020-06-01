package io.mamish.serverbot2.framework.server;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.google.gson.Gson;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SqsApiServer<ModelType> {

    private static final String THREAD_NAME = "SqsApiRequestReceiverThread";

    private final JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(getHandlerInstance(),getModelClass());
    private final SqsClient sqsClient = SqsClient.create();
    private final String serviceInterfaceName = getModelClass().getSimpleName();
    private final Logger logger = Logger.getLogger(getClass().getSimpleName());
    private final String receiveQueueName;

    protected abstract Class<ModelType> getModelClass();
    protected abstract ModelType getHandlerInstance();

    private final Gson gson = new Gson();

    public SqsApiServer(String receiveQueueName) {
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
            AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());

            final String receiveQueueUrl = sqsClient.getQueueUrl(r -> r.queueName(receiveQueueName)).queueUrl();

            while(true) {
                System.out.println("Polling messages");
                ReceiveMessageResponse response = sqsClient.receiveMessage(r ->
                        r.queueUrl(receiveQueueUrl)
                        .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS)
                );
                if (!response.hasMessages()) {
                    System.out.println("No messages received");
                } else {
                    response.messages().forEach(m -> {
                        AWSXRay.beginSegment(serviceInterfaceName+"Server");
                        // Deletion is synchronous to make exception handling simpler, though it could be made async.
                        System.out.println("Dumping message:");
                        System.out.println(gson.toJson(m));
                        System.out.println("Dumping message attrs:");
                        System.out.println(m.messageAttributes());
                        sqsClient.deleteMessage(r -> r.queueUrl(receiveQueueUrl).receiptHandle(m.receiptHandle()));

                        // FIXME: NPE occurs here. No idea why since CWL confirms the attributes are included in send.

                        String replyQueueUrl = m.messageAttributes().get(ApiConfig.JSON_REQUEST_QUEUE_KEY).stringValue();
                        String requestId = m.messageAttributes().get(ApiConfig.JSON_REQUEST_ID_KEY).stringValue();

                        AWSXRay.beginSubsegment("MakeRequest");
                        String responseString = jsonApiHandler.handleRequest(m.body());
                        AWSXRay.endSubsegment();

                        Map<String,MessageAttributeValue> sqsAttrMap = Map.of(
                                ApiConfig.JSON_REQUEST_ID_KEY, stringAttribute(requestId)
                                // No point putting the queue URL attribute back in.
                        );

                        AWSXRay.beginSubsegment("SendReply");
                        try {
                            sqsClient.sendMessage(r -> r.queueUrl(replyQueueUrl)
                                    .messageAttributes(sqsAttrMap)
                                    .messageBody(responseString));
                        } catch (Exception e) {
                            // Reply queue send might fail outside the control of this service, so don't make it fatal.
                            logger.log(Level.WARNING, "Unable to send SQS response message", e);
                            AWSXRay.getCurrentSubsegment().addException(e);
                        } finally {
                            AWSXRay.endSubsegment();
                        }

                        AWSXRay.endSegment();
                    });
                }
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
