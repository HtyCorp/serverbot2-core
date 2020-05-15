package io.mamish.serverbot2.networksecurity.model;

public class CreateSecurityGroupResponse {

    private String createdGroupId;

    public CreateSecurityGroupResponse() { }

    public CreateSecurityGroupResponse(String createdGroupId) {
        this.createdGroupId = createdGroupId;
    }

    public String getCreatedGroupId() {
        return createdGroupId;
    }
}
