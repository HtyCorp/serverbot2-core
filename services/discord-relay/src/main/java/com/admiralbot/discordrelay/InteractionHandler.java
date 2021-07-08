package com.admiralbot.discordrelay;

import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.exception.client.ApiClientException;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.Utils;
import com.amazonaws.xray.AWSXRay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InteractionHandler implements SlashCommandCreateListener {

    private static final String MSG_CAN_ONLY_USE_IN_CHANNELS = "Sorry, you can only use this command in a serverbot channel.";
    private static final String MSG_PRIVATE_REPLY_FAILED = "Sorry, sending the requested private message went wrong for some reason.";
    private static final String MSG_COMMANDSERVICE_CLIENT_EXCEPTION = "Sorry, something went wrong (unable to submit command).";
    private static final String MSG_COMMANDSERVICE_SERVER_EXCEPTION = "Sorry, something went wrong (failed to process command).";
    private static final String MSG_COMMANDSERVICE_OTHER_EXCEPTION = "Sorry, something went wrong (an unexpected error occurred).";
    private static final String MSG_GENERIC_COMMAND_SUCCESS = "Command completed successfully";

    private static final long DISCORD_ACTION_TIMEOUT_SECONDS = 5;

    private static final Logger logger = LogManager.getLogger(InteractionHandler.class);

    private final Executor threadPool = Executors.newCachedThreadPool();

    private final ChannelMap channelMap;
    private final DynamoMessageTable messageTable;
    private final ICommandService commandServiceClient;

    public InteractionHandler(ChannelMap channelMap, DynamoMessageTable messageTable, ICommandService commandServiceClient) {
        this.channelMap = channelMap;
        this.messageTable = messageTable;
        this.commandServiceClient = commandServiceClient;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent slashCommandCreateEvent) {
        threadPool.execute(() -> {
            try {
                AWSXRay.beginSegment("ProcessSlashCommandInteraction");
                handleSlashCommandInteraction(slashCommandCreateEvent.getSlashCommandInteraction());
            } catch (Exception e) {
                logger.error("Uncaught exception during slash command handling", e);
                AWSXRay.getCurrentSegment().addException(e);
            } finally {
                AWSXRay.endSegment();
            }
        });
    }

    /*
     * This is basically an adapted copy-and-paste from the classic DiscordRelay message handler. This is deliberately
     * not refactored in any smart way since slash message handling will completely replace message commands once
     * everything works; the replaced code will just be removed.
     */

    private void handleSlashCommandInteraction(SlashCommandInteraction interaction) {

        User requester = interaction.getUser();

        Optional<ServerTextChannel> maybeChannel = interaction.getChannel().flatMap(Channel::asServerTextChannel);
        if (maybeChannel.isEmpty()) {
            interaction.createImmediateResponder().append(MSG_CAN_ONLY_USE_IN_CHANNELS).respond();
            return;
        }
        ServerTextChannel channel = maybeChannel.get();

        Optional<MessageChannel> maybeAppChannel = channelMap.getAppChannel(channel);
        if (maybeAppChannel.isEmpty()) {
            interaction.createImmediateResponder().append(MSG_CAN_ONLY_USE_IN_CHANNELS).respond();
            return;
        }
        MessageChannel appChannel = maybeAppChannel.get();

        logger.info("Slash command interaction is valid, submitting command to CommandService...");

        CompletableFuture<InteractionOriginalResponseUpdater> responseUpdaterFuture = interaction.respondLater();

        final List<String> args = Utils.mapList(interaction.getOptions(),
                option -> option.getStringValue().orElseThrow());
        final String commandSourceId = Joiner.colon("slashcommand", interaction.getIdAsString());

        ProcessUserCommandResponse commandResponse = null;
        String errorReplyContent = MSG_COMMANDSERVICE_OTHER_EXCEPTION;

        try {
            commandResponse = AWSXRay.createSubsegment("SubmitCommand", subsegment -> {
                subsegment.putAnnotation("CommandSourceId", commandSourceId);
                ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(
                        args, appChannel, commandSourceId,
                        requester.getIdAsString(), requester.getDiscriminatedName()
                );
                return commandServiceClient.processUserCommand(commandRequest);
            });
        } catch (Exception e) {
            logger.error("Uncaught exception during CommandService command submit", e);
            if (e instanceof ApiClientException) {
                errorReplyContent = MSG_COMMANDSERVICE_CLIENT_EXCEPTION;
            } else if (e instanceof ApiServerException) {
                errorReplyContent = MSG_COMMANDSERVICE_SERVER_EXCEPTION;
            } else {
                errorReplyContent = MSG_COMMANDSERVICE_OTHER_EXCEPTION;
            }
        }

        // Wait for completion of the initial response
        InteractionOriginalResponseUpdater responseUpdater;
        try {
            responseUpdater = responseUpdaterFuture.get(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Initial response failed. Aborting since we can't contact user", e);
            return;
        }

        String replyMessageContent;
        String replyMessageExternalId = null;

        if (commandResponse == null) {

            logger.warn("Setting reply content to error message from failed CommandService call");
            replyMessageContent = errorReplyContent;

        } else {

            // Reply content and (nullable) ID from the command response are used if everything goes well
            logger.info("Setting reply content to message/ID returned by CommandService");
            replyMessageContent = commandResponse.getMessageContent();
            replyMessageExternalId = commandResponse.getMessageExternalId();

            // If there's a requested private reply as well, send it and overwrite the response message with a generic
            // "private reply failed" message if it doesn't succeed.
            if (commandResponse.getPrivateMessageContent() != null) {
                logger.info("Sending private reply message to sender");
                String pmContent = commandResponse.getPrivateMessageContent();
                EmbedBuilder pmEmbed = Utils.mapNullable(commandResponse.getPrivateMessageEmbed(), this::convertSimpleEmbed);
                try {
                    AWSXRay.createSubsegment("SendPrivateReply", () -> {
                        PrivateChannel requesterPrivateChannel = requester.openPrivateChannel()
                                .orTimeout(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .join();
                        requesterPrivateChannel.sendMessage(pmContent, pmEmbed)
                                .orTimeout(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .join();
                    });
                } catch (Exception e) {
                    logger.error("Failed to send request private message", e);
                    replyMessageContent = MSG_PRIVATE_REPLY_FAILED;
                    replyMessageExternalId = null;
                }
            }

            // If after all this the content is still null, that means the everything worked but the command did not
            // specify any reply content (no commands currently do this but it's technically supported).
            if (replyMessageContent == null) {
                logger.info("Setting reply content to generic completion message since CommandService returned none");
                replyMessageContent = MSG_GENERIC_COMMAND_SUCCESS;
                replyMessageExternalId = null;
            }

        }

        final String finalReplyContent = replyMessageContent;
        final String finalReplyExternalId = replyMessageExternalId;

        logger.info("Updating responder content");
        Message responseMessage = AWSXRay.createSubsegment("EditChannelReply", () ->
             responseUpdater.setContent(finalReplyContent).update()
                     .orTimeout(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).join()
        );
        if (replyMessageExternalId != null) {
            logger.info("Recording message ID in DDB to enable future tracking and edits");
            AWSXRay.createSubsegment("RecordMessageId", () -> {
                DynamoMessageItem newItem = new DynamoMessageItem(
                        finalReplyExternalId,
                        channel.getIdAsString(),
                        responseMessage.getIdAsString(),
                        interaction.getIdAsString(),
                        interaction.getToken()
                );
                LogUtils.debugDump(logger, "New DDB message item is: ", newItem);
                messageTable.put(newItem);
            });
        }
    }

    private EmbedBuilder convertSimpleEmbed(SimpleEmbed embed) {
        return new EmbedBuilder()
                .setUrl(embed.getUrl())
                .setTitle(embed.getTitle())
                .setDescription(embed.getDescription());
    }

}
