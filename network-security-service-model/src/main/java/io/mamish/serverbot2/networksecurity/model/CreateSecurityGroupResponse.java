package io.mamish.serverbot2.networksecurity.model;

public class CreateSecurityGroupResponse {

    private SecurityGroup createdGroup;

    public CreateSecurityGroupResponse() { }

    public CreateSecurityGroupResponse(SecurityGroup createdGroup) {
        this.createdGroup = createdGroup;
    }

    public SecurityGroup getCreatedGroup() {
        return createdGroup;
    }
}
