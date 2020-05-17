package io.mamish.serverbot2.networksecurity.model;

public class ModifyPortsResponse {

    private ApplicationSecurityGroup modifiedGroup;

    public ModifyPortsResponse() { }

    public ModifyPortsResponse(ApplicationSecurityGroup modifiedGroup) {
        this.modifiedGroup = modifiedGroup;
    }

    public ApplicationSecurityGroup getModifiedGroup() {
        return modifiedGroup;
    }
}
