package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.EditMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordServiceHandler;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.reflect.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordServiceHandler implements IDiscordServiceHandler {

    private DiscordApi discordApi;
    private ChannelMap channelMap;
    private JsonRequestDispatcher<IDiscordServiceHandler> requestDispatcher;
    private MessageDynamoTable messageDynamoTable;

    private Logger logger;

    public DiscordServiceHandler(DiscordApi discordApi, ChannelMap channelMap) {
        this.discordApi = discordApi;
        this.channelMap = channelMap;

        requestDispatcher = new JsonRequestDispatcher<>(this, IDiscordServiceHandler.class);
        messageDynamoTable = new MessageDynamoTable();
        logger = Logger.getLogger("DiscordServiceHandler");
    }

    public void handleRequest(String messageBody) {
        try {
            requestDispatcher.dispatch(messageBody);
        } catch (UnknownRequestException e) {
            logger.log(Level.WARNING, "Request method not recognised", e);
        } catch (RequestValidationException e) {
            logger.log(Level.WARNING, "Request contained insufficient or invalid parameters", e);
        } catch (RequestHandlingException e) {
            logger.log(Level.WARNING, "Handler threw exception while handling discord relay request", e);
        } catch (RequestHandlingRuntimeException e) {
            logger.log(Level.WARNING, "Uncaught exception while handling discord relay request", e);
        }
    }

    @Override
    public void onRequestNewMessage(NewMessageRequest newMessageRequest) {

        MessageChannel requestedChannel = newMessageRequest.getRecipientChannel();
        String requestedUserId = newMessageRequest.getRecipientUserId();
        String requestedContent = newMessageRequest.getContent();
        String requestedExternalId = newMessageRequest.getExternalId();

        if (requestedChannel != null && requestedUserId != null) {
            throw new RequestValidationException("Received NewMessageRequest with both a recipient channel and recipient user");
        }
        if (requestedChannel == null && requestedUserId == null) {
            throw new RequestValidationException("Received NewMessageRequest with neither a recipient channel or recipient user");
        }

        TextChannel channel;
        try {
            if (requestedChannel != null) {
                channel = channelMap.getDiscordChannel(requestedChannel).get();
            } else {
                channel = discordApi.getUserById(requestedUserId).thenCompose(User::openPrivateChannel).join();
            }
        } catch (Exception e) {
            throw new RequestHandlingException("Got valid NewMessageRequest recipient params, but failed to look up the user or channel");
        }

        // If no external ID supplied, send a message with no tracking in DynamoDB.
        if (newMessageRequest.getExternalId() == null) {
            channel.sendMessage(requestedContent).join();
        } else {
            if (messageDynamoTable.getItem(requestedExternalId) != null) {
                // This interface doesn't return a response so just log errors locally.
                throw new RequestValidationException("Received NewMessageRequest with an already used external ID");
            }
            Message message = channel.sendMessage(requestedContent).join();
            messageDynamoTable.putItem(new DynamoMessageItem(newMessageRequest.getExternalId(), message.getIdAsString()));
        }

    }

    @Override
    public void onRequestEditMessage(EditMessageRequest editMessageRequest) {

        // TODO

    }
}
