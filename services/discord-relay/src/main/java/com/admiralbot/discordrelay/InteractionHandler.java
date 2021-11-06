package com.admiralbot.discordrelay;

import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.exception.client.ApiClientException;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.Utils;
import com.admiralbot.sharedutil.XrayUtils;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InteractionHandler implements SlashCommandCreateListener {

    private static final String MSG_CAN_ONLY_USE_IN_CHANNELS = "Sorry, you can only use this command in a server bot channel " +
            "(due to Discord's limitations on slash command permissions).";
    private static final String MSG_PRIVATE_REPLY_FAILED = "Sorry, sending the requested private message went wrong for some reason.";
    private static final String MSG_COMMANDSERVICE_CLIENT_EXCEPTION = "Sorry, something went wrong (unable to submit command).";
    private static final String MSG_COMMANDSERVICE_SERVER_EXCEPTION = "Sorry, something went wrong (failed to process command).";
    private static final String MSG_COMMANDSERVICE_OTHER_EXCEPTION = "Sorry, something went wrong (an unexpected error occurred).";
    private static final String MSG_GENERIC_COMMAND_SUCCESS = "Command completed successfully";

    private static final long DISCORD_ACTION_TIMEOUT_SECONDS = 5;

    /*
    private static final List<String> CHOICES_MSG_PLEASE_WAIT = List.of(
            "Just a second...",
            "Just a moment...",
            "Hang on a tick...",
            "One second love...",
            "Working on it...",
            "Wait a bit...",
            "Almost there..."
    );
     */

    private static final Logger logger = LoggerFactory.getLogger(InteractionHandler.class);

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
                XrayUtils.beginSegment("ProcessSlashCommandInteraction");
                handleSlashCommandInteraction(slashCommandCreateEvent.getSlashCommandInteraction());
            } catch (Exception e) {
                logger.error("Uncaught exception during slash command handling", e);
                XrayUtils.addSegmentException(e);
            } finally {
                XrayUtils.endSegment();
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

        logger.info("Interaction details = {" +
                "commandName={}, " +
                "commandId={}, " +
                "id={}, " +
                "token={}, " +
                "channel={}, " +
                "type={}, " +
                "server={}, " +
                "user={}}",
                interaction.getCommandName(),
                interaction.getCommandIdAsString(),
                interaction.getIdAsString(),
                interaction.getToken(),
                interaction.getChannel().map(DiscordEntity::getIdAsString),
                interaction.getType().name(),
                interaction.getServer().map(DiscordEntity::getIdAsString),
                interaction.getUser().getIdAsString());

        if (interaction.getServer().isEmpty()) {
            logger.info("Interaction outside of a guild, ignoring");
            return;
        }
        String serverId = interaction.getServer().get().getIdAsString();
        if (!serverId.equals(DiscordConfig.DISCORD_SERVER_ID.getValue())) {
            logger.info("Interaction outside of assigned guild, ignoring");
            return;
        }

        Optional<ServerTextChannel> maybeChannel = interaction.getChannel().flatMap(Channel::asServerTextChannel);
        logger.info("maybeChannel={}", maybeChannel);
        if (maybeChannel.isEmpty()) {
            logger.info("Interaction in a non-text channel (interaction channel ID = {}",
                    interaction.getChannel().map(DiscordEntity::getIdAsString));
            interaction.createImmediateResponder().append(MSG_CAN_ONLY_USE_IN_CHANNELS).respond();
            return;
        }
        ServerTextChannel channel = maybeChannel.get();

        Optional<MessageChannel> maybeAppChannel = channelMap.getAppChannel(channel);
        logger.info("maybeAppChannel={}", maybeAppChannel);
        if (maybeAppChannel.isEmpty()) {
            logger.info("Interaction in a non-serverbot channel (interaction channel ID = {}",
                    interaction.getChannel().map(DiscordEntity::getIdAsString));
            interaction.createImmediateResponder().append(MSG_CAN_ONLY_USE_IN_CHANNELS).respond();
            return;
        }
        MessageChannel appChannel = maybeAppChannel.get();

        logger.info("Slash command interaction is valid, submitting command to CommandService...");

        // Ephemeral messages aren't deletable! Always something with Discord isn't there...
        /*
        String randomWaitMessage = CHOICES_MSG_PLEASE_WAIT.get(ThreadLocalRandom.current().nextInt(CHOICES_MSG_PLEASE_WAIT.size()));
        CompletableFuture<InteractionOriginalResponseUpdater> immediateResponseFuture = interaction.createImmediateResponder()
                .setContent(randomWaitMessage)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
         */
        CompletableFuture<InteractionOriginalResponseUpdater> immediateResponseFuture = interaction.respondLater();

        final List<String> commandWords = new ArrayList<>();
        commandWords.add(interaction.getCommandName());
        interaction.getOptions().forEach(option -> commandWords.add(option.getStringValue().orElseThrow()));

        final String commandSourceId = "i" + interaction.getIdAsString();

        ProcessUserCommandResponse commandResponse = null;
        String errorReplyContent = MSG_COMMANDSERVICE_OTHER_EXCEPTION;

        try {
            Map<String,Object> annotations = Map.of("CommandSourceId", commandSourceId);
            commandResponse = XrayUtils.subsegment("SubmitCommand", annotations, () -> {
                ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(
                        commandWords, appChannel, commandSourceId,
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
        InteractionOriginalResponseUpdater immediateResponse;
        try {
            immediateResponse = immediateResponseFuture.get(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Initial response failed. Aborting since we can't contact user", e);
            return;
        }

        String replyMessageContent;
        boolean ephemeralMessage = false;
        String replyMessageExternalId = null;

        if (commandResponse == null) {

            logger.warn("Setting reply content to error message from failed CommandService call");
            replyMessageContent = errorReplyContent;

        } else {

            // Reply content and (nullable) ID from the command response are used if everything goes well
            logger.info("Setting reply content to message/ID returned by CommandService");
            replyMessageContent = commandResponse.getMessageContent();
            ephemeralMessage = commandResponse.isEphemeralMessage();
            replyMessageExternalId = commandResponse.getMessageExternalId();

            // If there's a requested private reply as well, send it and overwrite the response message with a generic
            // "private reply failed" message if it doesn't succeed.
            if (commandResponse.getPrivateMessageContent() != null) {
                logger.info("Sending private reply message to sender");
                String pmContent = commandResponse.getPrivateMessageContent();
                EmbedBuilder pmEmbed = Utils.mapNullable(commandResponse.getPrivateMessageEmbed(), this::convertSimpleEmbed);
                try {
                    XrayUtils.subsegment("SendPrivateReply", null, () -> {
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

            // If after all this the content is still null, that means everything worked but the command did not
            // specify any reply content (no commands currently do this, but it's technically supported).
            if (replyMessageContent == null) {
                logger.info("Setting reply content to generic completion message since CommandService returned none");
                replyMessageContent = MSG_GENERIC_COMMAND_SUCCESS;
                replyMessageExternalId = null;
            }

        }

        final String finalReplyContent = replyMessageContent;
        final String finalReplyExternalId = replyMessageExternalId;

        logger.info("Sending followup response...");

        boolean finalEphemeralMessage = ephemeralMessage;
        Message responseMessage = XrayUtils.subsegment("SendFollowupResponse", null, () -> {
            // We could theoretically just edit the immediate response when the new response is also non-ephemeral,
            // but I prefer to do things consistently whether it's ephemeral or not.

            //immediateResponse.delete();
            InteractionFollowupMessageBuilder followupMessage = interaction.createFollowupMessageBuilder();
            followupMessage.setContent(finalReplyContent);
            if (finalEphemeralMessage) {
                followupMessage.setFlags(MessageFlag.EPHEMERAL);
            }
            return followupMessage.send().orTimeout(DISCORD_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        });

        if (replyMessageExternalId != null) {
            logger.info("Recording interaction details in DDB to enable future tracking and edits");
            XrayUtils.subsegment("StoreInteractionDetails", null, () -> {
                DynamoMessageItem newItem = new DynamoMessageItem(
                        finalReplyExternalId,
                        channel.getIdAsString(),
                        responseMessage.getIdAsString(),
                        interaction.getIdAsString(),
                        interaction.getToken(),
                        finalReplyContent
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