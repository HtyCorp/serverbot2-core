package com.admiralbot.discordrelay;

import com.admiralbot.discordrelay.model.service.*;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestHandlingRuntimeException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.server.HttpApiServer;
import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.Utils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class RelayServiceHandler extends HttpApiServer<IDiscordService> implements IDiscordService {

    private final DiscordApi discordApi;
    private final ChannelMap channelMap;
    private final DynamoMessageTable messageTable;
    private final SlashCommandUpdater slashCommandUpdater;

    private final Logger logger = LoggerFactory.getLogger(RelayServiceHandler.class);

    @Override
    protected Class<IDiscordService> getModelClass() {
        return IDiscordService.class;
    }

    @Override
    protected IDiscordService createHandlerInstance() {
        return this;
    }

    public RelayServiceHandler(DiscordApi discordApi, ChannelMap channelMap, DynamoMessageTable messageTable,
                               SlashCommandUpdater slashCommandUpdater) {
        this.discordApi = discordApi;
        this.channelMap = channelMap;
        this.messageTable = messageTable;
        this.slashCommandUpdater = slashCommandUpdater;
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
                    message.getIdAsString(), requestedContent));
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

        // DDB is used as the source of truth for message content: fetching message content by message ID doesn't seem
        // to work for original interaction responses so this is more reliable
        String oldContent = dbItem.getMessageContent();
        String newContent = null;
        if (editMode == EditMode.REPLACE) {
            newContent = requestedContent;
        } else if (editMode == EditMode.APPEND) {
            newContent = oldContent + "\n" + requestedContent;
        }

        // Edit method changes depending on whether the message is sent from an interaction or not
        String interactionId = dbItem.getInteractionId();
        String interactionToken = dbItem.getInteractionToken();
        if (interactionId != null && interactionToken != null) {
            InteractionResponder interactionEditor = new InteractionResponder(discordApi);
            interactionEditor.setContent(newContent);
            interactionEditor.editFollowupMessage(interactionId, interactionToken, messageId).join();
        } else {
            message.edit(newContent).join();
        }

        // Update the message content in DDB; this should happen last since DDB is the source of truth and we don't want
        // faults to make phantom changes to it (e.g. if an APPEND call was retried several times)
        dbItem.setMessageContent(newContent);
        messageTable.put(dbItem);

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
        Role targetRole = discordApi.getRoleById(roleId).orElseThrow(() -> new IllegalStateException(
                "Role with ID '"+roleId+"' could not be located to modify"
        ));

        Function<User, CompletableFuture<Void>> operation;
        if (modifyOperation == RoleModifyOperation.ADD_USER) operation = targetRole::addUser;
        else if (modifyOperation == RoleModifyOperation.REMOVE_USER) operation = targetRole::removeUser;
        else throw new RequestHandlingRuntimeException("Impossible state: invalid operation type");

        // Note that this acts like a PUT and will still succeed even if the user already has this role added/removed.
        operation.apply(targetUser).join();

        return new ModifyRoleMembershipResponse();

    }

    @Override
    public PutSlashCommandsResponse putSlashCommands(PutSlashCommandsRequest request) {
        slashCommandUpdater.putSlashCommands(request.getSlashCommands());
        return new PutSlashCommandsResponse();
    }

}
