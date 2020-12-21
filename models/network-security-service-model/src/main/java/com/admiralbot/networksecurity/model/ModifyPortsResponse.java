package com.admiralbot.networksecurity.model;

public class ModifyPortsResponse {

    private ManagedSecurityGroup modifiedGroup;

    public ModifyPortsResponse() { }

    public ModifyPortsResponse(ManagedSecurityGroup modifiedGroup) {
        this.modifiedGroup = modifiedGroup;
    }

    public ManagedSecurityGroup getModifiedGroup() {
        return modifiedGroup;
    }
}
