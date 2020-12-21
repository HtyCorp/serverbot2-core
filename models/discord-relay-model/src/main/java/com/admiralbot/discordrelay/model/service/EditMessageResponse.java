package com.admiralbot.discordrelay.model.service;

public class EditMessageResponse {

    private String newMessageContent;
    private String discordRealChannelId;
    private String discordRealMessageId;

    public EditMessageResponse() { }

    public EditMessageResponse(String editedMessageContent, String discordRealChannelId, String discordRealMessageId) {
        this.newMessageContent = editedMessageContent;
        this.discordRealChannelId = discordRealChannelId;
        this.discordRealMessageId = discordRealMessageId;
    }

    public String getNewMessageContent() {
        return newMessageContent;
    }

    public String getDiscordRealChannelId() {
        return discordRealChannelId;
    }

    public String getDiscordRealMessageId() {
        return discordRealMessageId;
    }
}
