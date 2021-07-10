package com.admiralbot.discordrelay;

import com.admiralbot.commandservice.model.GenerateSlashCommandSetRequest;
import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.XrayUtils;
import com.amazonaws.xray.AWSXRay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;
import java.util.Optional;
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

    private static final long LOGIN_TIMEOUT_SECONDS = 30;
    private static final long MESSAGE_ACTION_TIMEOUT_SECONDS = 4;

    // https://javacord.org/wiki/basic-tutorials/gateway-intents.html#list-of-intents
    // We only want message events (and interactions), but Javacord requires GUILDS too (login fails without it)
    private static final Intent[] REQUIRED_DISCORD_INTENTS = new Intent[]{
            Intent.GUILDS,
            Intent.GUILD_MESSAGES
    };

    private final Logger logger = LogManager.getLogger(DiscordRelay.class);

    private final CommandArgParser commandArgParser;
    private final ChannelMap channelMap;

    // Javacord event dispatching isn't designed to handle long-running listeners, so we offload to an Executor
    private final Executor handlerQueue;

    public DiscordRelay() {

        commandArgParser = new CommandArgParser();
        handlerQueue = Executors.newCachedThreadPool();

        logger.info("Building CommandService client...");
        ICommandService commandServiceClient = ApiClient.http(ICommandService.class);

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
        DynamoMessageTable messageTable = new DynamoMessageTable();

        logger.info("Building slash command updater and updating commands...");
        SlashCommandUpdater slashCommandUpdater = new SlashCommandUpdater(discordApi, channelMap);
        slashCommandUpdater.putSlashCommands(commandServiceClient.generateSlashCommandSet(
                new GenerateSlashCommandSetRequest()).getSlashCommands());

        logger.info("Builder interaction handler...");
        InteractionHandler slashCommandHandler = new InteractionHandler(channelMap, messageTable, commandServiceClient);

        logger.info("Starting API service handler...");
        new RelayServiceHandler(discordApi, channelMap, messageTable, slashCommandUpdater);

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

        // This was the sigil used before slash commands were supported
        final String OLD_SIGIL = "!";
        if (!content.startsWith(OLD_SIGIL)) {
            logIgnoreMessageReason(receivedMessage, "missing command sigil character");
            return;
        }

        String rawInput = content.substring(OLD_SIGIL.length()); // Take everything after sigil
        List<String> words = commandArgParser.parseArgs(rawInput);
        if (words.size() < 1 || words.get(0).length() == 0) {
            logIgnoreMessageReason(receivedMessage,"no command immediately after sigil character");
            return;
        }

        new MessageBuilder()
                .append(requesterUser)
                .append(" Admiralbot now supports slash commands! Try typing ")
                .append("/", MessageDecoration.CODE_SIMPLE)
                .append(" to run your command or see what commands are available.")
                .send(channel)
                .orTimeout(MESSAGE_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }
}
