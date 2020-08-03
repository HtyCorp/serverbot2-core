package io.mamish.serverbot2.discordrelay;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.client.ApiClient;
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
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DiscordRelay {

    public static void main(String[] args) {
        System.out.println("Running relay v2020-01-06T22:15Z10...");
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        new DiscordRelay();
    }

    private final Logger logger = LogManager.getLogger(DiscordRelay.class);

    private final CommandArgParser commandArgParser;
    private final ChannelMap channelMap;
    private final ICommandService commandServiceClient;
    private final DynamoMessageTable messageTable;

    public DiscordRelay() {
        commandArgParser = new CommandArgParser();
        commandServiceClient = ApiClient.lambda(ICommandService.class, CommandLambdaConfig.FUNCTION_NAME);
        String apiToken = DiscordConfig.API_TOKEN.getValue();
        DiscordApi discordApi = new DiscordApiBuilder().setToken(apiToken).login().join();
        channelMap = new ChannelMap(discordApi);
        messageTable = new DynamoMessageTable();
        new DiscordServiceHandler(discordApi, channelMap, messageTable);
        discordApi.addMessageCreateListener(this::traceMessageCreate);
    }

    private void traceMessageCreate(MessageCreateEvent messageCreateEvent) {
        try {
            AWSXRay.beginSegment("ProcessUserMessage");
            onMessageCreate(messageCreateEvent);
        } catch (Exception e) {
            e.printStackTrace();
            AWSXRay.getCurrentSegment().addException(e);
        } finally {
            AWSXRay.endSegment();
        }
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
        if (oAppChannel.isEmpty() || oAppChannel.get().equals(MessageChannel.DEBUG)) {
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

        AWSXRay.beginSubsegment("SubmitUserCommand");
        AWSXRay.getCurrentSegment().putAnnotation("DiscordMessageId", receivedMessage.getIdAsString());
        ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(
                words,
                oAppChannel.get(),
                receivedMessage.getIdAsString(),
                author.getIdAsString(),
                author.getDiscriminatedName());
        ProcessUserCommandResponse commandResponse = commandServiceClient.processUserCommand(commandRequest);
        AWSXRay.endSubsegment();

        AWSXRay.beginSubsegment("RespondToUser");
        if (commandResponse.getOptionalMessageContent() != null) {
            Message newMessage = channel.sendMessage(commandResponse.getOptionalMessageContent()).join();
            String newMessageExtId = commandResponse.getOptionalMessageExternalId();
            if (newMessageExtId != null) {
                DynamoMessageItem newItem = new DynamoMessageItem(newMessageExtId, channel.getIdAsString(), newMessage.getIdAsString());
                logger.debug("Content is " + commandResponse.getOptionalMessageContent());
                LogUtils.debugDump(logger, "New item is: ", newItem);
                messageTable.put(newItem);
            }
        }
        AWSXRay.endSubsegment();

    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }

}
