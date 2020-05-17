package io.mamish.serverbot2.networksecurity.model;

public class CreateSecurityGroupResponse {

    private ApplicationSecurityGroup createdGroup;

    public CreateSecurityGroupResponse() { }

    public CreateSecurityGroupResponse(ApplicationSecurityGroup createdGroup) {
        this.createdGroup = createdGroup;
    }

    public ApplicationSecurityGroup getCreatedGroup() {
        return createdGroup;
    }
}
