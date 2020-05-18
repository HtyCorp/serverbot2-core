package io.mamish.serverbot2.networksecurity.model;

public class CreateSecurityGroupResponse {

    private ManagedSecurityGroup createdGroup;

    public CreateSecurityGroupResponse() { }

    public CreateSecurityGroupResponse(ManagedSecurityGroup createdGroup) {
        this.createdGroup = createdGroup;
    }

    public ManagedSecurityGroup getCreatedGroup() {
        return createdGroup;
    }
}
