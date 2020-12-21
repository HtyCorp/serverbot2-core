package com.admiralbot.networksecurity.firewall;

public class DecryptedPrefixListEntry {

    private String cidr;
    private DiscordUserAuthInfo userInfo;

    public DecryptedPrefixListEntry(String cidr, DiscordUserAuthInfo userInfo) {
        this.cidr = cidr;
        this.userInfo = userInfo;
    }

    public String getCidr() {
        return cidr;
    }

    public DiscordUserAuthInfo getUserInfo() {
        return userInfo;
    }
}
