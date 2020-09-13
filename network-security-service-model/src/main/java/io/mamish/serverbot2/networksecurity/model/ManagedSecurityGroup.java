package io.mamish.serverbot2.networksecurity.model;

import java.util.List;

public class ManagedSecurityGroup {

    private String name;
    private String groupId;
    private List<PortPermission> allowedPorts;

    public ManagedSecurityGroup() { }

    public ManagedSecurityGroup(String name, String groupId) {
        this.name = name;
        this.groupId = groupId;
    }

    public ManagedSecurityGroup(String name, String groupId, List<PortPermission> allowedPorts) {
        this.name = name;
        this.groupId = groupId;
        this.allowedPorts = allowedPorts;
    }

    public String getName() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<PortPermission> getAllowedPorts() {
        return allowedPorts;
    }

}
