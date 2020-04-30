package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.reflect.DdbAttribute;
import io.mamish.serverbot2.sharedutil.reflect.DdbKeyType;

public class DynamoMessageItem {

    @DdbAttribute(value = DiscordConfig.MESSAGE_TABLE_PKEY, keyType = DdbKeyType.PARTITION)
    private String externalMessageId;
    @DdbAttribute("DiscordId")
    private String discordMessageId;

    public DynamoMessageItem() {}

    public DynamoMessageItem(String externalMessageId, String discordMessageId) {
        this.externalMessageId = externalMessageId;
        this.discordMessageId = discordMessageId;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public String getDiscordMessageId() {
        return discordMessageId;
    }
}
