package io.mamish.serverbot2.discordrelay.model.service;

import java.util.List;

public class NewMessageRequest {

    private String externalId;
    private List<MessageChannel> channels;
    private String recipientUserId;
    private String content;

    public NewMessageRequest(String externalId, List<MessageChannel> channels, String recipientUserId, String content) {
        this.externalId = externalId;
        this.channels = channels;
        this.recipientUserId = recipientUserId;
        this.content = content;
    }

    public String getExternalId() {
        return externalId;
    }

    public List<MessageChannel> getChannels() {
        return channels;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getContent() {
        return content;
    }
}
