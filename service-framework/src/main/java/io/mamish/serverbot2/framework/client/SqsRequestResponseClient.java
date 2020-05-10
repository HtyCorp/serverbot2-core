package io.mamish.serverbot2.framework.client;

import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SqsRequestResponseClient {

    private final SqsAsyncClient realSqsAsyncClient = SqsAsyncClient.create();
    private final SqsClient realSqsClient = SqsClient.create();
    private final String rxTempQueueUrl = realSqsClient.createQueue(r ->
            r.queueName(generateQueueName())).queueUrl();
    private final Map<String, Queue<String>> requestIdToSync =
            Collections.synchronizedMap(new HashMap<>());

    public SqsRequestResponseClient() {
        Thread receiverThread = new Thread(this::runReceiveLoop);
        receiverThread.setDaemon(true);
        receiverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                realSqsAsyncClient.deleteQueue(r -> r.queueUrl(rxTempQueueUrl))));
    }

    public String getQueueUrl(String queueName) {
        return realSqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
    }

    public String sendAndReceive(String queueUrl, String messageBody, int timeoutSeconds, String requestId) {

        // Could potentially use SynchronousQueue instead.
        // Would need to start async receive before send to guarantee correctness (e.g. with CompletableFuture).
        ArrayBlockingQueue<String> sync = new ArrayBlockingQueue<>(1);
        requestIdToSync.put(requestId, sync);

        // Send message with request ID in attributes.
        Map<String, MessageAttributeValue> sqsAttrMap = Map.of(
                ApiConfig.JSON_REQUEST_QUEUE_KEY, stringAttribute(rxTempQueueUrl),
                ApiConfig.JSON_REQUEST_ID_KEY, stringAttribute(requestId));
        realSqsClient.sendMessage(r -> r.messageBody(messageBody)
                .messageAttributes(sqsAttrMap)
                .queueUrl(queueUrl));

        // Wake receiver thread if it's idle.
        synchronized (this) {
            notifyAll();
        }

        // Poll for the queue response or time out if taking too long.
        String response;
        try {
            response = sync.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (response == null) {
                throw new ApiClientTimeoutException(requestId);
            }
            return response;
        } catch (InterruptedException e) {
            throw new ApiClientTimeoutException(requestId, e);
        } finally {
            // Note removal may be performed by receiver thread first.
            requestIdToSync.remove(requestId);
        }
    }

    private  void runReceiveLoop() {
        while(true) {

            // Sleep if no requests pending.
            synchronized (this) {
                while(requestIdToSync.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // No fancy logic needed if this gets interrupted.
                        // Thread is private to class.
                        throw new RuntimeException(e);
                    }
                }
            }

            // Receive messages
            List<Message> messages = realSqsClient.receiveMessage(r -> r.queueUrl(rxTempQueueUrl)
                .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS)).messages();

            // For reach message, remove the queue for that request ID from the map and offer the message to it.
            messages.forEach(m -> {
                String requestId = m.messageAttributes().get(ApiConfig.JSON_REQUEST_ID_KEY).stringValue();
                // Note removal may be performed by `sendAndReceive` caller first.
                Queue<String> receiveQueue = requestIdToSync.remove(requestId);
                if (receiveQueue != null) {
                    receiveQueue.offer(m.body());
                }
            });

            // Delete all messages
            var deleteList = messages.stream().map(m -> DeleteMessageBatchRequestEntry.builder()
                    .receiptHandle(m.receiptHandle())
                    .id(m.messageId()) // We ignore return statuses anyway so this just has to be unique
                    .build()
            ).collect(Collectors.toList());
            realSqsClient.deleteMessageBatch(r -> r.entries(deleteList));

        }
    }

    private static String generateQueueName() {
        // Should be < 70 chars, so within the 80-char limit for SQS queue names.
        return "api-client-temp-" + IDUtils.epochSeconds() + "-" + IDUtils.randomUUIDJoined();
    }

    private static MessageAttributeValue stringAttribute(String s) {
        return MessageAttributeValue.builder().stringValue(s).build();
    }

}
