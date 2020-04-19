package io.mamish.serverbot2.commandlambda.model.service;

import java.util.List;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CommandService", numRequiredFields = 3, description = "Run a user command submitted from Discord")
public class CommandServiceRequest {

    @ApiArgumentInfo(order = 0, name = "words", description = "List of whitespace-split words in user command")
    private List<String> words;
    @ApiArgumentInfo(order = 1, name = "channel", description = "Abstracted channel command was sent to")
    private MessageChannel channel;
    @ApiArgumentInfo(order = 2, name = "senderId", description = "Discord ID of sending user")
    private String senderId;

    @SuppressWarnings("unused")
    public CommandServiceRequest() {}

    public CommandServiceRequest(List<String> words, MessageChannel channel, String senderId) {
        this.words = words;
        this.channel = channel;
        this.senderId = senderId;
    }

    public List<String> getWords() {
        return words;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getSenderId() {
        return senderId;
    }
}
