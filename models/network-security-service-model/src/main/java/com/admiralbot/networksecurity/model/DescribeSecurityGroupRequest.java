package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

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
