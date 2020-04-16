package io.mamish.serverbot2.commandlambda.model.service;

import java.util.List;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;

public class UserCommandRequest {

    private List<String> words;
    private MessageChannel channel;
    private String senderId;

    public UserCommandRequest(List<String> words, MessageChannel channel, String senderId) {
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
