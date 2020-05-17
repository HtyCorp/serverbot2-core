package io.mamish.serverbot2.networksecurity.model;

public class DiscordUserIp {

    private String discordId;
    private String ipAddress;

    public DiscordUserIp() { }

    public DiscordUserIp(String discordId, String ipAddress) {
        this.discordId = discordId;
        this.ipAddress = ipAddress;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
