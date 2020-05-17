package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CreateSecurityGroup", numRequiredFields = 3, description = "Create a dynamic security group which updates its rules when users authorize their IP using !addip")
public class CreateSecurityGroupRequest {

    @ApiArgumentInfo(order = 0, description = "Unique name for group, nominally same as game short name")
    private String gameName;

    public CreateSecurityGroupRequest() { }

    public CreateSecurityGroupRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }

}
