package io.mamish.serverbot2.networksecurity.model;

public class DeleteSecurityGroupResponse {

    private String deletedSecurityGroupId;

    public DeleteSecurityGroupResponse() { }

    public DeleteSecurityGroupResponse(String deletedSecurityGroupId) {
        this.deletedSecurityGroupId = deletedSecurityGroupId;
    }

    public String getDeletedSecurityGroupId() {
        return deletedSecurityGroupId;
    }
}
