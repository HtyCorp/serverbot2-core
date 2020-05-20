package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.dynamomapper.DynamoKey;
import io.mamish.serverbot2.dynamomapper.DynamoKeyType;

public class DynamoMessageItem {

    @DynamoKey(DynamoKeyType.PARTITION)
    private String externalMessageId;
    private String discordChannelId;
    private String discordMessageId;

    public DynamoMessageItem() {}

    public DynamoMessageItem(String externalMessageId, String discordChannelId, String discordMessageId) {
        this.externalMessageId = externalMessageId;
        this.discordChannelId = discordChannelId;
        this.discordMessageId = discordMessageId;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public String getDiscordMessageId() {
        return discordMessageId;
    }
}
