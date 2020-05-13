package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.reflect.DdbAttribute;
import io.mamish.serverbot2.sharedutil.reflect.DdbKeyType;

public class DynamoMessageItem {

    @DdbAttribute(value = DiscordConfig.MESSAGE_TABLE_PKEY, keyType = DdbKeyType.PARTITION)
    private String externalMessageId;
    @DdbAttribute("DiscordChannelId")
    private String discordChannelId;
    @DdbAttribute("DiscordMessageId")
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
