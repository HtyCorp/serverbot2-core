package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "DescribeSecurityGroup", numRequiredFields = 1, description = "Describe the (simplified) security group")
public class DescribeSecurityGroupRequest {

    @ApiArgumentInfo(order = 0, description = "Name of group to describe")
    private String gameName;

    public DescribeSecurityGroupRequest() { }

    public DescribeSecurityGroupRequest(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }
}
