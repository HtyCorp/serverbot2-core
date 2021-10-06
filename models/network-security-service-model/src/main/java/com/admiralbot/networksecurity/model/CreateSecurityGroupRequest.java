package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CreateSecurityGroup", numRequiredFields = 1,
        description = "Create a dynamic security group which updates its rules when users authorize their IP using /addip")
public class CreateSecurityGroupRequest {

    @ApiArgumentInfo(order = 0, description = "Unique name for the group, typically matching a game ID")
    private String gameName;

    public CreateSecurityGroupRequest() { }

    public CreateSecurityGroupRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }

}
