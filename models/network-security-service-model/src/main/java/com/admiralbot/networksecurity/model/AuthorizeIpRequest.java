package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "AuthorizeIp", numRequiredFields = 2,
        description = "Add inbound security group rules allowing this user IP, or replace the IP in their existing rules")
public class AuthorizeIpRequest {

    @ApiArgumentInfo(order = 0, description = "IPv4 address of user to allow connections from")
    private String userIpAddress;

    @ApiArgumentInfo(order = 1, description = "The encrypted user ID generated as part of a GenerateIpAuthUrl request")
    private String userAuthToken;

    public AuthorizeIpRequest() { }

    public AuthorizeIpRequest(String userIpAddress, String userAuthToken) {
        this.userIpAddress = userIpAddress;
        this.userAuthToken = userAuthToken;
    }

    public String getUserIpAddress() {
        return userIpAddress;
    }

    public String getUserAuthToken() {
        return userAuthToken;
    }
}
