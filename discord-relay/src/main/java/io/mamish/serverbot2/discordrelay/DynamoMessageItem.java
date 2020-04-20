package io.mamish.serverbot2.discordrelay;

public class DynamoMessageItem {

    private String externalMessageId;
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
