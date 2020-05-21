package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.*;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.server.SqsApiServer;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;
import java.util.logging.Logger;

public class DiscordServiceHandler extends SqsApiServer<IDiscordService> implements IDiscordService {

    private final DiscordApi discordApi;
    private final ChannelMap channelMap;
    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.create();
    private final DynamoDbTable<DynamoMessageItem> messageTable = ddbClient.table(DiscordConfig.MESSAGE_TABLE_NAME,
            TableSchema.fromBean(DynamoMessageItem.class));

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected Class<IDiscordService> getModelClass() {
        return IDiscordService.class;
    }

    @Override
    protected IDiscordService getHandlerInstance() {
        return this;
    }

    @Override
    protected String getReceiverQueueName() {
        return DiscordConfig.SQS_QUEUE_NAME;
    }

    public DiscordServiceHandler(DiscordApi discordApi, ChannelMap channelMap) {
        this.discordApi = discordApi;
        this.channelMap = channelMap;
    }

    @Override
    public NewMessageResponse requestNewMessage(NewMessageRequest newMessageRequest) {

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

        if (requestedChannel != null) {
            Optional<ServerTextChannel> optChannel = channelMap.getDiscordChannel(requestedChannel);
            if (optChannel.isPresent()) {
                channel = optChannel.get();
            } else {
                throw new RequestHandlingException("Discord channel missing for app MessageChannel " + requestedChannel);
            }
        } else {
            try {
                channel = discordApi.getUserById(requestedUserId).thenCompose(User::openPrivateChannel).join();
            } catch (Exception e) {
                throw new RequestHandlingException("Failed to open private channel to requested user with ID " + requestedUserId, e);
            }
        }

        if (requestedExternalId != null) {
            if (messageTable.getItem(partition(requestedExternalId)) != null) {
                throw new RequestValidationException("Received NewMessageRequest with an already used external ID");
            }
        }

        Message message = channel.sendMessage(requestedContent).join();

        if (requestedExternalId != null) {
            messageTable.putItem(new DynamoMessageItem(requestedExternalId, channel.getIdAsString(),
                    message.getIdAsString()));
        }

        return new NewMessageResponse(channel.getIdAsString(), message.getIdAsString());

    }

    @Override
    public EditMessageResponse requestEditMessage(EditMessageRequest editMessageRequest) {

        String requestedContent = editMessageRequest.getContent();
        String externalId = editMessageRequest.getExternalId();
        EditMode editMode = editMessageRequest.getEditMode();

        DynamoMessageItem dbItem = messageTable.getItem(partition(externalId));
        if (dbItem == null) {
            throw new RequestValidationException("No message entry found for external ID " + externalId);
        }
        String channelId = dbItem.getDiscordChannelId();
        String messageId = dbItem.getDiscordMessageId();

        TextChannel channel;
        Optional<TextChannel> optChannel = discordApi.getChannelById(channelId).flatMap(Channel::asTextChannel);
        if (optChannel.isEmpty()) {
            throw new RequestHandlingException("Could not retrieve original channel of message, from channel ID "
                    + channelId + " and message ID " + messageId);
        } else {
            channel = optChannel.get();
        }

        // Odd that this isn't an optional like Channel. Doesn't declare exceptions either.
        // Will have to test effect with deleted messages and see how logging/recovery can be improved.
        Message message = discordApi.getMessageById(messageId, channel).join();

        String oldContent = message.getContent();
        String newContent = null;
        if (editMode == EditMode.REPLACE) {
            newContent = requestedContent;
        } else if (editMode == EditMode.APPEND) {
            newContent = oldContent + "\n" + requestedContent;
        }
        message.edit(newContent);

        return new EditMessageResponse(newContent, channelId, messageId);

    }

    private static Key partition(String value) {
        return Key.builder().partitionValue(value).build();
    }

}
