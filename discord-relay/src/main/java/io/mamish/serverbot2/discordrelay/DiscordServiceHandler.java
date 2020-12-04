package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.*;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.server.SqsApiServer;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.Utils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

public class DiscordServiceHandler extends SqsApiServer<IDiscordService> implements IDiscordService {

    private final DiscordApi discordApi;
    private final ChannelMap channelMap;
    private final DynamoMessageTable messageTable;

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected Class<IDiscordService> getModelClass() {
        return IDiscordService.class;
    }

    @Override
    protected IDiscordService createHandlerInstance() {
        return this;
    }

    public DiscordServiceHandler(DiscordApi discordApi, ChannelMap channelMap, DynamoMessageTable messageTable) {
        super(DiscordConfig.SQS_QUEUE_NAME);
        this.discordApi = discordApi;
        this.channelMap = channelMap;
        this.messageTable = messageTable;
    }

    @Override
    public NewMessageResponse newMessage(NewMessageRequest newMessageRequest) {

        MessageChannel requestedChannel = newMessageRequest.getRecipientChannel();
        String requestedUserId = newMessageRequest.getRecipientUserId();
        String requestedContent = newMessageRequest.getContent();
        String requestedExternalId = newMessageRequest.getExternalId();
        SimpleEmbed requestedEmbed = newMessageRequest.getEmbed();

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
            if (messageTable.has(requestedExternalId)) {
                throw new RequestValidationException("Received NewMessageRequest with an already used external ID");
            }
        }

        Message message;
        if (newMessageRequest.getEmbed() != null) {
            message = channel.sendMessage(requestedContent, convertSimpleEmbed(requestedEmbed)).join();
        } else {
            message = channel.sendMessage(requestedContent).join();
        }

        if (requestedExternalId != null) {
            messageTable.put(new DynamoMessageItem(requestedExternalId, channel.getIdAsString(),
                    message.getIdAsString()));
        }

        return new NewMessageResponse(channel.getIdAsString(), message.getIdAsString());

    }

    private EmbedBuilder convertSimpleEmbed(SimpleEmbed embed) {
        return new EmbedBuilder()
                .setUrl(embed.getUrl())
                .setTitle(embed.getTitle())
                .setDescription(embed.getDescription());
    }

    @Override
    public EditMessageResponse editMessage(EditMessageRequest editMessageRequest) {

        String requestedContent = editMessageRequest.getContent();
        String externalId = editMessageRequest.getExternalId();
        EditMode editMode = editMessageRequest.getEditMode();

        DynamoMessageItem dbItem = messageTable.get(externalId);
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

    @Override
    public ModifyRoleMembershipResponse modifyRoleMembership(ModifyRoleMembershipRequest modifyRoleMembershipRequest) {
        MessageChannel channel = modifyRoleMembershipRequest.getRoleChannel();
        RoleModifyOperation modifyOperation = modifyRoleMembershipRequest.getRoleModifyOperation();

        if (channel != MessageChannel.MAIN) {
            throw new RequestValidationException("Requested channel " + channel + "does not support join/leave");
        }
        if (!Utils.equalsAny(modifyOperation, RoleModifyOperation.ADD_USER, RoleModifyOperation.REMOVE_USER)) {
            throw new RequestValidationException("Requested modification type " + modifyOperation + "not valid");
        }

        String roleId = DiscordConfig.CHANNEL_ROLE_MAIN.getValue();

        User targetUser = discordApi.getUserById(modifyRoleMembershipRequest.getUserDiscordId()).join();
        Role targetRole = discordApi.getRoleById(roleId).get();

        Function<User, CompletableFuture<Void>> operation;
        if (modifyOperation == RoleModifyOperation.ADD_USER) operation = targetRole::addUser;
        else if (modifyOperation == RoleModifyOperation.REMOVE_USER) operation = targetRole::removeUser;
        else throw new RequestHandlingRuntimeException("Impossible state: invalid operation type");

        // TODO: Determine how this responds if request doesn't change membership.
        operation.apply(targetUser).join();

        return new ModifyRoleMembershipResponse();

    }

}
