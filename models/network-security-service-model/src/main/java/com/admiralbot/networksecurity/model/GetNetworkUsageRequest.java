package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 20, name = "GetNetworkUsage", numRequiredFields = 3,
        description = "Queries flow logs for the given instance ID to generate usage statistics")
public class GetNetworkUsageRequest {

    @ApiArgumentInfo(order = 0, description = "Private VPC IP of network interface to examine")
    private String endpointVpcIp;

    @ApiArgumentInfo(order = 1, description = "Name of security group containing ports to check activity on")
    private String targetSecurityGroupName;

    @ApiArgumentInfo(order = 2, description = "Window of most recent minutes to examine")
    private int windowSeconds;

    public GetNetworkUsageRequest() { }

    public GetNetworkUsageRequest(String endpointVpcIp, String targetSecurityGroupName, int windowSeconds) {
        this.endpointVpcIp = endpointVpcIp;
        this.targetSecurityGroupName = targetSecurityGroupName;
        this.windowSeconds = windowSeconds;
    }

    public String getEndpointVpcIp() {
        return endpointVpcIp;
    }

    public String getTargetSecurityGroupName() {
        return targetSecurityGroupName;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }
}
