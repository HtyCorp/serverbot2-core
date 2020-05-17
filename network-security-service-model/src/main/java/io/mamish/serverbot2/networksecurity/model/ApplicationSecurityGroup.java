package io.mamish.serverbot2.networksecurity.model;

import java.util.List;

public class ApplicationSecurityGroup {

    private String gameName;
    private String groupId;
    private List<PortPermission> allowedPorts;
    private List<DiscordUserIp> allowedAddresses;

    public ApplicationSecurityGroup() { }

    public ApplicationSecurityGroup(String gameName, String groupId, List<PortPermission> allowedPorts, List<DiscordUserIp> allowedAddresses) {
        this.gameName = gameName;
        this.groupId = groupId;
        this.allowedPorts = allowedPorts;
        this.allowedAddresses = allowedAddresses;
    }

    public String getGameName() {
        return gameName;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<PortPermission> getAllowedPorts() {
        return allowedPorts;
    }

    public List<DiscordUserIp> getAllowedAddresses() {
        return allowedAddresses;
    }
}
