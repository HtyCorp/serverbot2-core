package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 12, name = "DeleteSecurityGroup", numRequiredFields = 1, description = "Delete a managed security group")
public class DeleteSecurityGroupRequest {

    @ApiArgumentInfo(order = 0, description = "Name of security group to delete")
    private String gameName;

    public DeleteSecurityGroupRequest() { }

    public DeleteSecurityGroupRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
