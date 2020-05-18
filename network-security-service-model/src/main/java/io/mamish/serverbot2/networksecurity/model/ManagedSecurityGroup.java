package io.mamish.serverbot2.networksecurity.model;

import java.util.List;

public class ManagedSecurityGroup {

    private String gameName;
    private String fullName;
    private String groupId;
    private String encryptedDataKey;
    private List<PortPermission> allowedPorts;
    private List<DiscordUserIp> allowedUsers;

    public ManagedSecurityGroup() { }

    public ManagedSecurityGroup(String gameName, String fullName, String groupId, String encryptedDataKey,
                                List<PortPermission> allowedPorts, List<DiscordUserIp> allowedUsers) {
        this.gameName = gameName;
        this.fullName = fullName;
        this.groupId = groupId;
        this.encryptedDataKey = encryptedDataKey;
        this.allowedPorts = allowedPorts;
        this.allowedUsers = allowedUsers;
    }

    public String getGameName() {
        return gameName;
    }

    public String getFullName() {
        return fullName;
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
