package com.admiralbot.discordrelay;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DynamoMessageItem {

    private String externalMessageId;
    private String discordChannelId;
    private String discordMessageId;

    public DynamoMessageItem() {}

    public DynamoMessageItem(String externalMessageId, String discordChannelId, String discordMessageId) {
        this.externalMessageId = externalMessageId;
        this.discordChannelId = discordChannelId;
        this.discordMessageId = discordMessageId;
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
}
