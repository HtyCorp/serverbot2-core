package io.mamish.serverbot2.networksecurity.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordUserIp that = (DiscordUserIp) o;
        return Objects.equals(discordId, that.discordId) &&
                Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discordId, ipAddress);
    }
}
