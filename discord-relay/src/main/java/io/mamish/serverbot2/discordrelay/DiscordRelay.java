package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
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
import java.util.logging.Logger;

public class DiscordRelay {

    public static void main(String[] args) {
        new DiscordRelay();
    }

    Logger logger = Logger.getLogger("DiscordRelay");

    private final ChannelMap channelMap;
    private final ICommandService commandServiceClient;
    private final DynamoMessageTable messageTable;

    public DiscordRelay() {
        commandServiceClient = ApiClient.lambda(ICommandService.class, CommandLambdaConfig.FUNCTION_NAME);
        String apiToken = DiscordConfig.API_TOKEN.getValue();
        DiscordApi discordApi = new DiscordApiBuilder().setToken(apiToken).login().join();
        channelMap = new ChannelMap(discordApi);
        messageTable = new DynamoMessageTable();
        new DiscordServiceHandler(discordApi, channelMap, messageTable);
        discordApi.addMessageCreateListener(this::onMessageCreate);
    }

    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
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

        List<String> words = Arrays.asList(content.substring(SIGIL.length()).split("\\s+"));
        if (words.size() < 1 || words.get(0).length() == 0) {
            logIgnoreMessageReason(receivedMessage,"no command immediately after sigil character");
            return;
        }

        ProcessUserCommandRequest commandRequest = new ProcessUserCommandRequest(words, oAppChannel.get(), author.getIdAsString());
        ProcessUserCommandResponse commandResponse = commandServiceClient.processUserCommand(commandRequest);

        if (commandResponse.getOptionalMessageContent() != null) {
            Message newMessage = channel.sendMessage(commandResponse.getOptionalMessageContent()).join();
            String newMessageExtId = commandResponse.getOptionalMessageExternalId();
            if (newMessageExtId != null) {
                messageTable.put(new DynamoMessageItem(newMessageExtId, channel.getIdAsString(), newMessage.getIdAsString()));
            }
        }

    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }

}
