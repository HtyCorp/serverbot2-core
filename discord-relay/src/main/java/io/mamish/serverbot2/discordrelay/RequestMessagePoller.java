package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.exception.server.UnknownRequestException;
import io.mamish.serverbot2.framework.server.JsonApiRequestDispatcher;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import org.javacord.api.DiscordApi;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestMessagePoller {

    private JsonApiRequestDispatcher<IDiscordService> requestDispatcher;
    private Logger logger;

    public RequestMessagePoller(DiscordApi discordApi, ChannelMap channelMap) {

        DiscordServiceHandler handlerInstance  = new DiscordServiceHandler(discordApi, channelMap);
        requestDispatcher = new JsonApiRequestDispatcher<>(handlerInstance, IDiscordService.class);
        logger = Logger.getLogger("DiscordServiceHandler");

        new Thread(this::loopMessagePoll, "DiscordRelaySQSPoller").start();
    }

    private void loopMessagePoll() {
        SqsClient sqsClient = SqsClient.create();
        final String QUEUE_URL = DiscordConfig.SQS_QUEUE_URL_RESOLVED.getValue();
        while(true) {
            ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r
                    .queueUrl(QUEUE_URL)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(CommonConfig.DEFAULT_SQS_WAIT_TIME_SECONDS));
            if (response.hasMessages()) {
                Message message = response.messages().get(0);
                runServiceHandler(message.body());
                sqsClient.deleteMessage(r -> r.queueUrl(QUEUE_URL).receiptHandle(message.receiptHandle()));
            }
        }
    }

    private void runServiceHandler(String messageBody) {
        try {
            requestDispatcher.handleRequest(messageBody);
        } catch (UnknownRequestException e) {
            logger.log(Level.SEVERE, "Request method not recognised", e);
        } catch (RequestValidationException e) {
            logger.log(Level.SEVERE, "Request contained insufficient or invalid parameters", e);
        } catch (RequestHandlingException e) {
            logger.log(Level.SEVERE, "Handler threw exception while handling discord relay request", e);
        } catch (RequestHandlingRuntimeException e) {
            logger.log(Level.SEVERE, "Uncaught exception while handling discord relay request", e);
        }
    }

}
