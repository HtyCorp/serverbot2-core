package com.admiralbot.discordrelay.model.service;

public class NewMessageResponse {

    private String discordRealChannelId;
    private String discordRealMessageId;

    public NewMessageResponse() { }

    public NewMessageResponse(String discordRealChannelId, String discordRealMessageId) {
        this.discordRealChannelId = discordRealChannelId;
        this.discordRealMessageId = discordRealMessageId;
    }

    public String getDiscordRealChannelId() {
        return discordRealChannelId;
    }

    public String getDiscordRealMessageId() {
        return discordRealMessageId;
    }
}
