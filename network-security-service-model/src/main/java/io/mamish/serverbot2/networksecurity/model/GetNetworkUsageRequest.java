package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 20, name = "GetNetworkUsage", numRequiredFields = 2,
        description = "Queries flow logs for the given instance ID to generate usage statistics")
public class GetNetworkUsageRequest {

    @ApiArgumentInfo(order = 0, description = "Private VPC IP of network interface to examine")
    private String endpointVpcIp;

    @ApiArgumentInfo(order = 1, description = "Window of most recent minutes to examine")
    private int windowSeconds;

    public GetNetworkUsageRequest() { }

    public GetNetworkUsageRequest(String endpointVpcIp, int windowSeconds) {
        this.endpointVpcIp = endpointVpcIp;
        this.windowSeconds = windowSeconds;
    }

    public String getEndpointVpcIp() {
        return endpointVpcIp;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }
}