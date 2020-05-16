package io.mamish.serverbot2.networksecurity.model;

import java.util.List;

public class SecurityGroup {

    private String gameName;
    private String groupId;
    private List<PortPermission> allowedPorts;
    private List<String> allowedAddresses;

    public SecurityGroup() { }

    public SecurityGroup(String gameName, String groupId, List<PortPermission> allowedPorts, List<String> allowedAddresses) {
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

    public List<String> getAllowedAddresses() {
        return allowedAddresses;
    }
}
