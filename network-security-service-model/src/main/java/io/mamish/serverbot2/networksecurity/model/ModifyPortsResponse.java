package io.mamish.serverbot2.networksecurity.model;

public class ModifyPortsResponse {

    private SecurityGroup modifiedGroup;

    public ModifyPortsResponse() { }

    public ModifyPortsResponse(SecurityGroup modifiedGroup) {
        this.modifiedGroup = modifiedGroup;
    }

    public SecurityGroup getModifiedGroup() {
        return modifiedGroup;
    }
}
