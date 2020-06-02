package io.mamish.serverbot2.framework.server;

import com.amazonaws.services.xray.model.Trace;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.google.gson.Gson;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

public abstract class SqsApiServer<ModelType> {

    private static final String THREAD_NAME = "SqsApiRequestReceiverThread";

    private final ModelType handlerInstance = createHandlerInstance();
    private final JsonApiRequestDispatcher<ModelType> jsonApiHandler = new JsonApiRequestDispatcher<>(handlerInstance,getModelClass());

    private final SqsClient sqsClient = SqsClient.create();
    private final String serviceInterfaceName = getModelClass().getSimpleName();
    private final String receiveQueueName;

    private final Logger logger = LogManager.getLogger(SqsApiServer.class);
    private final Gson gson = new Gson();

    /**
     * <p>
     * Must return the class of generic parameter <code>ModelType</code>.
     * <p>
     * Due to type erasure, the class corresponding to a given generic parameter can't be retrieved dynamically at
     * runtime, so it needs to be explicitly provided by the subclass.
     *
     * @return The class of generic parameter <code>ModelType</code>
     */
    protected abstract Class<ModelType> getModelClass();

    /**
     * <p>
     * Create a new instance of <code>ModelType</code> to handle API requests for the given service model.
     * <p>
     * Warning: this is called during the super constructor in LambdaApiServer, which runs <b>before</b> any instance
     * field initialization in the subclass. You cannot refer to any instance fields since they will be null at this
     * point.
     * <p>
     * This class will attempt to parse the payload of Lambda invocations as requests in the given service, and dispatch
     * them to the provided handler.
     *
     * @return An instance of <code>ModelType</code> to handle API requests
     */
    protected abstract ModelType createHandlerInstance();

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

            final List<String> receiveAttributeNames = List.of(
                    ApiConfig.JSON_REQUEST_ID_KEY,
                    ApiConfig.JSON_REQUEST_QUEUE_KEY
            );

            while(true) {
                logger.debug("Polling messages");
                ReceiveMessageResponse response = sqsClient.receiveMessage(r ->
                        r.queueUrl(receiveQueueUrl)
                        .messageAttributeNames(receiveAttributeNames)
                        .attributeNames(QueueAttributeName.ALL)
                        .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS)
                );
                if (!response.hasMessages()) {
                    logger.debug("No messages received");
                } else {
                    response.messages().forEach(message -> {

                        // Propagate Xray trace information if available in message attributes
                        TraceHeader trace = extractTraceHeaderIfAvailable(message);
                        if (trace != null) {
                            AWSXRay.beginSegment(serviceInterfaceName+"Server", trace.getRootTraceId(), trace.getParentId());
                        } else {
                            AWSXRay.beginSegment(serviceInterfaceName+"Server");
                        }

                        try {

                            if (!message.hasAttributes()) {
                                logger.debug("Message does not have any message attributes.");
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

                            logger.debug(LogUtils.dump("Dumping message:", message));
                            logger.debug(LogUtils.dump("Dumping message attrs:", message.messageAttributes()));
                            logger.debug(LogUtils.dump("Dumping message system attrs:", message.attributes()));

                            sqsClient.deleteMessage(r -> r.queueUrl(receiveQueueUrl).receiptHandle(message.receiptHandle()));

                            AWSXRay.beginSubsegment("DispatchRequest");
                            String responseString = jsonApiHandler.handleRequest(message.body());
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
                                logger.warn("Unable to send SQS response message", e);
                                AWSXRay.getCurrentSubsegment().addException(e);
                            } finally {
                                AWSXRay.endSubsegment();
                            }

                        } finally {
                            AWSXRay.endSegment();
                        }
                    });
                }
            }
        } catch (SdkException e) {
            // Can't (or shouldn't) recover from operations failures on our own queue. Let the thread die instead.
            logger.warn("Fatal error in SQS receive loop", e);
        }
    }

    private TraceHeader extractTraceHeaderIfAvailable(Message message) {
        if (message.hasAttributes()) {
            String headerString = message.attributes().get(MessageSystemAttributeName.AWS_TRACE_HEADER);
            if (headerString != null) {
                return TraceHeader.fromString(headerString);
            }
        }
        return null;
    }

    private static MessageAttributeValue stringAttribute(String s) {
        return MessageAttributeValue.builder().stringValue(s).dataType("String").build();
    }

}
