package io.mamish.serverbot2.discordrelay;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.client.ApiClientException;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiscordRelay {

    public static void main(String[] args) {
        System.out.println("Launching Discord relay...");
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        new DiscordRelay();
    }

    private final static long LOGIN_TIMEOUT_SECONDS = 30;
    private final static long DISCORD_MESSAGE_RESPOND_TIMEOUT_SECONDS = 5;

    private static final String ERR_COMMANDSERVICE_CLIENT_EXCEPTION = "Sorry, something went wrong (unable to submit command).";
    private static final String ERR_COMMANDSERVICE_SERVER_EXCEPTION = "Sorry, something went wrong (failed to process command).";
    private static final String ERR_COMMANDSERVICE_OTHER_EXCEPTION = "Sorry, something went wrong (an unexpected error occurred).";

    // https://javacord.org/wiki/basic-tutorials/gateway-intents.html#list-of-intents
    // We only want message events, but Javacord requires GUILDS too (login fails without it)
    private static final Intent[] REQUIRED_DISCORD_INTENTS = new Intent[]{
            Intent.GUILDS,
            Intent.GUILD_MESSAGES
    };

    private final Logger logger = LogManager.getLogger(DiscordRelay.class);

    private final CommandArgParser commandArgParser;
    private final ChannelMap channelMap;
    private final ICommandService commandServiceClient;
    private final DynamoMessageTable messageTable;
    private final Executor messageHandlerExecutor;

    public DiscordRelay() {

        commandArgParser = new CommandArgParser();
        messageHandlerExecutor = Executors.newCachedThreadPool();

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

        logger.info("Starting SQS service handler...");
        new DiscordServiceHandler(discordApi, channelMap, messageTable);

        logger.info("Registering Javacord message listener...");
        discordApi.addMessageCreateListener(this::queueHandleMessageCreateEvent);

        logger.info("Ready to receive messages and API calls");
    }

    // Javacord warns against using listener threads for long-running tasks, so hand off to an Executor instead.
    private void queueHandleMessageCreateEvent(MessageCreateEvent messageCreateEvent) {
        messageHandlerExecutor.execute(() -> {
            try {
                AWSXRay.beginSegment("ProcessUserMessage");
                onMessageCreate(messageCreateEvent);
            } catch (Exception e) {
                logger.error("Uncaught exception during Discord message handling", e);
                AWSXRay.getCurrentSegment().addException(e);
            } finally {
                AWSXRay.endSegment();
            }
        });
    }

    private void onMessageCreate(MessageCreateEvent messageCreateEvent) {

        Message receivedMessage = messageCreateEvent.getMessage();
        MessageAuthor author = messageCreateEvent.getMessageAuthor();
        Channel abstractChannel = messageCreateEvent.getChannel();
        String content = messageCreateEvent.getMessageContent();

        ServerTextChannel channel;
        if (abstractChannel.asServerTextChannel().isEmpty()) {
            logIgnoreMessageReason(receivedMessage, "not from a server text channel");
            return;
        }
        channel = abstractChannel.asServerTextChannel().get();

        if (author.isYourself()) {
            logIgnoreMessageReason(receivedMessage, "sent by self");
            return;
        }

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

        logger.info("Message passed all checks for command validity, submitting to CommandService...");

        String replyMessageContent;
        String replyMessageExternalId = null;

        try {
            AWSXRay.beginSubsegment("SubmitUserCommand");
            AWSXRay.getCurrentSegment().putAnnotation("DiscordMessageId", receivedMessage.getIdAsString());
            ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(
                    words,
                    oAppChannel.get(),
                    receivedMessage.getIdAsString(),
                    author.getIdAsString(),
                    author.getDiscriminatedName());
            ProcessUserCommandResponse commandResponse = commandServiceClient.processUserCommand(commandRequest);
            replyMessageContent = commandResponse.getOptionalMessageContent();
            replyMessageExternalId = commandResponse.getOptionalMessageExternalId();
            AWSXRay.endSubsegment();
        } catch (Exception e) {
            logger.error("Uncaught exception during CommandService command submit", e);
            if (e instanceof ApiClientException) {
                replyMessageContent = ERR_COMMANDSERVICE_CLIENT_EXCEPTION;
            } else if (e instanceof ApiServerException) {
                replyMessageContent = ERR_COMMANDSERVICE_SERVER_EXCEPTION;
            } else {
                replyMessageContent = ERR_COMMANDSERVICE_OTHER_EXCEPTION;
            }
            AWSXRay.getCurrentSegment().addException(e);
        } finally {
            AWSXRay.endSubsegment();
        }

        if (replyMessageContent == null) {
            logger.info("No command reply message received");
        } else {
            logger.info("Responding to user...");
            try {
                AWSXRay.beginSubsegment("RespondToUser");
                Message newMessage = channel.sendMessage(replyMessageContent)
                        .orTimeout(DISCORD_MESSAGE_RESPOND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .join();
                if (replyMessageExternalId != null) {
                    DynamoMessageItem newItem = new DynamoMessageItem(replyMessageExternalId, channel.getIdAsString(), newMessage.getIdAsString());
                    logger.debug("Content is " + replyMessageContent);
                    LogUtils.debugDump(logger, "New item is: ", newItem);
                    messageTable.put(newItem);
                }
                AWSXRay.endSubsegment();
            } catch (Exception e) {
                logger.error("Uncaught exception while responding to user", e);
                AWSXRay.getCurrentSegment().addException(e);
            } finally {
                AWSXRay.endSubsegment();
            }
        }

    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }

}
