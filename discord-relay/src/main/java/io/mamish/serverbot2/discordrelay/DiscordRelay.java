package io.mamish.serverbot2.discordrelay;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DiscordRelay {

    Logger logger = Logger.getLogger("DiscordRelay");

    private Gson gson = new Gson();
    private LambdaClient lambdaClient = LambdaClient.builder().region(CommonConfig.REGION).build();
    private DiscordApi discordApi;
    private ChannelMap channelMap;

    public DiscordRelay() {
        String apiToken = DiscordConfig.API_TOKEN.getValue();
        discordApi = new DiscordApiBuilder().setToken(apiToken).login().join();

        channelMap = new ChannelMap(discordApi);

        discordApi.addMessageCreateListener(this::onMessageCreate);
    }

    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        Message message = messageCreateEvent.getMessage();
        MessageAuthor author = messageCreateEvent.getMessageAuthor();
        Channel abstractChannel = messageCreateEvent.getChannel();
        String content = messageCreateEvent.getMessageContent();

        ServerTextChannel channel;
        if (abstractChannel.asServerTextChannel().isEmpty()) {
            logIgnoreMessageReason(message, "not from a server text channel");
            return;
        }
        channel = abstractChannel.asServerTextChannel().get();

        if (author.isYourself()) {
            logIgnoreMessageReason(message, "sent by self");
            return;
        }

        Optional<MessageChannel> oAppChannel = channelMap.getAppChannel(channel);
        if (oAppChannel.isEmpty() || oAppChannel.get().equals(MessageChannel.DEBUG)) {
            logIgnoreMessageReason(message, "not in a response-enabled channel");
            return;
        }

        final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;
        if (!content.startsWith(SIGIL)) {
            logIgnoreMessageReason(message, "missing command sigil character");
            return;
        }

        List<String> words = Arrays.asList(content.substring(SIGIL.length()).split("\\s+"));
        if (words.size() < 1 || words.get(0).length() == 0) {
            logIgnoreMessageReason(message,"no command immediately after sigil character");
            return;
        }

        CommandServiceRequest commandRequest = new CommandServiceRequest(words, oAppChannel.get(), author.getIdAsString());
        CommandServiceResponse commandResponse = invokeCommandLambda(commandRequest);

        if (commandResponse.getOptionalMessageContent() != null) {
            channel.sendMessage(commandResponse.getOptionalMessageContent());
        }

    }

    private CommandServiceResponse invokeCommandLambda(CommandServiceRequest request) {
        SdkBytes requestPayload = SdkBytes.fromUtf8String(gson.toJson(request));
        InvokeResponse response = lambdaClient.invoke(r -> r.payload(requestPayload).functionName(CommandLambdaConfig.FUNCTION_NAME));
        return gson.fromJson(response.payload().asUtf8String(), CommandServiceResponse.class);
    }

    private void logIgnoreMessageReason(Message message, String reason) {
        logger.info("Ignored message " + message.getIdAsString() + ": " + reason + ".");
    }

}
