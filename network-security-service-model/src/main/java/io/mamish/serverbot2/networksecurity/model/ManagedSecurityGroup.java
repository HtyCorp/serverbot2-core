package io.mamish.serverbot2.networksecurity.model;

import java.util.List;

public class ManagedSecurityGroup {

    private String name;
    private String groupId;
    private String encryptedDataKey;
    private List<PortPermission> allowedPorts;
    private List<DiscordUserIp> allowedUsers;

    public ManagedSecurityGroup() { }

    public ManagedSecurityGroup(String name, String groupId, String encryptedDataKey) {
        this.name = name;
        this.groupId = groupId;
        this.encryptedDataKey = encryptedDataKey;
    }

    public ManagedSecurityGroup(String name, String groupId, String encryptedDataKey,
                                List<PortPermission> allowedPorts, List<DiscordUserIp> allowedUsers) {
        this.name = name;
        this.groupId = groupId;
        this.encryptedDataKey = encryptedDataKey;
        this.allowedPorts = allowedPorts;
        this.allowedUsers = allowedUsers;
    }

    public String getName() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getEncryptedDataKey() {
        return encryptedDataKey;
    }

    public List<PortPermission> getAllowedPorts() {
        return allowedPorts;
    }

    public List<DiscordUserIp> getAllowedUsers() {
        return allowedUsers;
    }
}
