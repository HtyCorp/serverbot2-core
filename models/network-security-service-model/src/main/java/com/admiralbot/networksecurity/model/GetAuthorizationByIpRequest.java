package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 20, name = "GetAuthorizationByIp", numRequiredFields = 1,
        description = "Check if the input IP address is authorized")
public class GetAuthorizationByIpRequest {

    @ApiArgumentInfo(order = 0, description = "The IP address to check")
    private String ipAddress;

    public GetAuthorizationByIpRequest() { }

    public GetAuthorizationByIpRequest(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

}
