package com.admiralbot.discordrelay;

import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.client.ApiClientException;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.*;
import com.amazonaws.xray.AWSXRay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.Interaction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DiscordRelay {

    public static void main(String[] args) {
        XrayUtils.setIgnoreMissingContext();
        XrayUtils.setServiceName("DiscordRelay");
        AppContext.setContainer();
        new DiscordRelay();
    }

    private final static long LOGIN_TIMEOUT_SECONDS = 30;
    private final static long MESSAGE_ACTION_TIMEOUT_SECONDS = 4;

    private static final String MSG_WORKING_ON_IT = "Working on it...";
    private static final String MSG_GENERIC_COMMAND_SUCCESS = "Command completed successfully";
    private static final String ERR_PRIVATE_REPLY_FAILED = "Sorry, sending the requested private message went wrong for some reason.";
    private static final String ERR_COMMANDSERVICE_CLIENT_EXCEPTION = "Sorry, something went wrong (unable to submit command).";
    private static final String ERR_COMMANDSERVICE_SERVER_EXCEPTION = "Sorry, something went wrong (failed to process command).";
    private static final String ERR_COMMANDSERVICE_OTHER_EXCEPTION = "Sorry, something went wrong (an unexpected error occurred).";

    // https://javacord.org/wiki/basic-tutorials/gateway-intents.html#list-of-intents
    // We only want message events (and interactions), but Javacord requires GUILDS too (login fails without it)
    private static final Intent[] REQUIRED_DISCORD_INTENTS = new Intent[]{
            Intent.GUILDS,
            Intent.GUILD_MESSAGES
    };

    private final Logger logger = LogManager.getLogger(DiscordRelay.class);

    private final CommandArgParser commandArgParser;
    private final ChannelMap channelMap;
    private final ICommandService commandServiceClient;
    private final DynamoMessageTable messageTable;

    // Javacord event dispatching isn't designed to handle long-running listeners, so we offload to an Executor
    private final Executor handlerQueue;

    public DiscordRelay() {

        commandArgParser = new CommandArgParser();
        handlerQueue = Executors.newCachedThreadPool();

        logger.info("Building CommandService client...");
        commandServiceClient = ApiClient.http(ICommandService.class);

        logger.info("Logging in to Discord API...");
        String apiToken = DiscordConfig.API_TOKEN.getValue();
        DiscordApi discordApi = new DiscordApiBuilder()
                .setToken(apiToken)
                .setIntents(REQUIRED_DISCORD_INTENTS)
                .login()
                .orTimeout(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();

        logger.info("Building channel map...");
        channelMap = new ChannelMap(discordApi);

        logger.info("Building DDB message table");
        messageTable = new DynamoMessageTable();

        logger.info("Builder interaction handler...");
        InteractionHandler slashCommandHandler = new InteractionHandler(channelMap, messageTable, commandServiceClient);

        logger.info("Starting API service handler...");
        new RelayServiceHandler(discordApi, channelMap, messageTable);

        logger.info("Registering Javacord listeners...");
        discordApi.addMessageCreateListener(event -> asyncExecute("ProcessUserMessage",
                this::onMessageCreate, event));
        discordApi.addSlashCommandCreateListener(slashCommandHandler);

        logger.info("Ready to receive messages and API calls");
    }

    private <T> void asyncExecute(String segmentName, Consumer<T> handler, T event) {
        handlerQueue.execute(() -> {
            try {
                AWSXRay.beginSegment(segmentName);
                handler.accept(event);
            } catch (Exception e) {
                logger.error("Uncaught exception during {} handling", segmentName, e);
                AWSXRay.getCurrentSegment().addException(e);
            } finally {
                AWSXRay.endSegment();
            }
        });
    }

    private void onMessageCreate(MessageCreateEvent messageCreateEvent) {

        // Get basic important message details

        Message receivedMessage = messageCreateEvent.getMessage();
        MessageAuthor messageAuthor = messageCreateEvent.getMessageAuthor();
        Channel abstractChannel = messageCreateEvent.getChannel();
        String content = messageCreateEvent.getMessageContent();

        // Start process message details and parse required details as we go (sender, channel, etc)

        ServerTextChannel channel;
        if (abstractChannel.asServerTextChannel().isEmpty()) {
            logIgnoreMessageReason(receivedMessage, "not from a server text channel");
            return;
        }
        channel = abstractChannel.asServerTextChannel().get();

        if (messageAuthor.isYourself()) {
            logIgnoreMessageReason(receivedMessage, "sent by self");
            return;
        }

        if (!messageAuthor.isRegularUser()) {
            logIgnoreMessageReason(receivedMessage, "not sent by regular-type user");
            return;
        }
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        User requesterUser = messageAuthor.asUser().get();

        Optional<MessageChannel> oAppChannel = channelMap.getAppChannel(channel);
        if (oAppChannel.isEmpty()) {
            logIgnoreMessageReason(receivedMessage, "not in a response-enabled channel");
            return;
        }

        final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;
        if (!content.startsWith(SIGIL)) {
            logIgnoreMessageReason(receivedMessage, "missing command sigil character");
            return;
        }

        String rawInput = content.substring(SIGIL.length()); // Take everything after sigil
        List<String> words = commandArgParser.parseArgs(rawInput);
        if (words.size() < 1 || words.get(0).length() == 0) {
            logIgnoreMessageReason(receivedMessage,"no command immediately after sigil character");
            return;
        }

        logger.info("Message passed all checks for command validity. Sending initial reply message.");

        String commandSourceId = Joiner.colon("message", receivedMessage.getIdAsString());
        invokeCommand(requesterUser, channel, oAppChannel.get(), words, commandSourceId);

    }

    private void invokeCommand(User requester, ServerTextChannel discordChannel, MessageChannel appChannel,
                               List<String> words, String commandSourceId) {


        // Send initial message asynchronously so we can immediately start working on the command

        CompletableFuture<Message> initialMessageFuture = AWSXRay.createSubsegment("SendInitialReply",
                () -> discordChannel.sendMessage(MSG_WORKING_ON_IT)
        );

        // Submit to CommandService

        logger.info("Submitting command to CommandService...");

        ProcessUserCommandResponse commandResponse = null;
        String errorReplyContent = ERR_COMMANDSERVICE_OTHER_EXCEPTION;

        try {
            commandResponse = AWSXRay.createSubsegment("SubmitCommand", subsegment -> {
                subsegment.putAnnotation("CommandSourceId", commandSourceId);
                ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(
                        words, appChannel, commandSourceId,
                        requester.getIdAsString(), requester.getDiscriminatedName()
                );
                return commandServiceClient.processUserCommand(commandRequest);
            });
        } catch (Exception e) {
            logger.error("Uncaught exception during CommandService command submit", e);
            if (e instanceof ApiClientException) {
                errorReplyContent = ERR_COMMANDSERVICE_CLIENT_EXCEPTION;
            } else if (e instanceof ApiServerException) {
                errorReplyContent = ERR_COMMANDSERVICE_SERVER_EXCEPTION;
            } else {
                errorReplyContent = ERR_COMMANDSERVICE_OTHER_EXCEPTION;
            }
        }

        // Get the initial message we sent asynchronously before

        Message replyMessage;
        try {
            replyMessage = initialMessageFuture.orTimeout(MESSAGE_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            logger.error("Initial reply message send failed. Aborting since we can't contact users", e);
            return;
        }

        // Decide what final message content to send based on any previous errors and the command response

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
                                .orTimeout(MESSAGE_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .join();
                        requesterPrivateChannel.sendMessage(pmContent, pmEmbed)
                                .orTimeout(MESSAGE_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .join();
                    });
                } catch (Exception e) {
                    logger.error("Failed to send request private message", e);
                    replyMessageContent = ERR_PRIVATE_REPLY_FAILED;
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

        logger.info("Issuing final reply message edit with decided content");
        AWSXRay.createSubsegment("EditChannelReply", () -> {
            replyMessage.edit(finalReplyContent)
                    .orTimeout(MESSAGE_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        });
        if (replyMessageExternalId != null) {
            logger.info("Recording message ID in DDB to enable future tracking and edits");
            AWSXRay.createSubsegment("RecordMessageId", () -> {
                DynamoMessageItem newItem = new DynamoMessageItem(
                        finalReplyExternalId,
                        discordChannel.getIdAsString(),
                        replyMessage.getIdAsString(),
                        finalReplyContent
                );
                LogUtils.debugDump(logger, "New DDB message item is: ", newItem);
                messageTable.put(newItem);
            });
        }
    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }

    private EmbedBuilder convertSimpleEmbed(SimpleEmbed embed) {
        return new EmbedBuilder()
                .setUrl(embed.getUrl())
                .setTitle(embed.getTitle())
                .setDescription(embed.getDescription());
    }

}
