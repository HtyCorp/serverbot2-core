package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.EditMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordServiceHandler;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.framework.exception.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.RequestValidationException;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.reflect.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import java.util.logging.Logger;

public class DiscordServiceHandler implements IDiscordServiceHandler {

    private DiscordApi discordApi;
    private ChannelMap channelMap;
    private SimpleDynamoDbMapper<DynamoMessageItem> messageMapper;

    private Logger logger;

    public DiscordServiceHandler(DiscordApi discordApi, ChannelMap channelMap) {
        this.discordApi = discordApi;
        this.channelMap = channelMap;
        messageMapper = new SimpleDynamoDbMapper<>(DiscordConfig.MESSAGE_TABLE_NAME, DynamoMessageItem.class);
        logger = Logger.getLogger("DiscordServiceHandler");
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
            if (messageMapper.has(requestedExternalId)) {
                // This interface doesn't return a response so just log errors locally.
                throw new RequestValidationException("Received NewMessageRequest with an already used external ID");
            }
            Message message = channel.sendMessage(requestedContent).join();
            messageMapper.put(new DynamoMessageItem(newMessageRequest.getExternalId(), message.getIdAsString()));
        }

    }

    @Override
    public void onRequestEditMessage(EditMessageRequest editMessageRequest) {

        // TODO

    }
}
