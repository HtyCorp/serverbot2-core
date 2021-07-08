package com.admiralbot.discordrelay;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DynamoMessageItem {

    private String externalMessageId;
    private String discordChannelId;
    private String discordMessageId;
    private String interactionId;
    private String interactionToken;

    public DynamoMessageItem() {}

    public DynamoMessageItem(String externalMessageId, String discordChannelId, String discordMessageId) {
        this.externalMessageId = externalMessageId;
        this.discordChannelId = discordChannelId;
        this.discordMessageId = discordMessageId;
    }

    public DynamoMessageItem(String externalMessageId, String discordChannelId, String discordMessageId,
                             String interactionId, String interactionToken) {
        this.externalMessageId = externalMessageId;
        this.discordChannelId = discordChannelId;
        this.discordMessageId = discordMessageId;
        this.interactionId = interactionId;
        this.interactionToken = interactionToken;
    }

    @DynamoDbPartitionKey
    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public void setDiscordChannelId(String discordChannelId) {
        this.discordChannelId = discordChannelId;
    }

    public String getDiscordMessageId() {
        return discordMessageId;
    }

    public void setDiscordMessageId(String discordMessageId) {
        this.discordMessageId = discordMessageId;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getInteractionToken() {
        return interactionToken;
    }

    public void setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
    }
}
