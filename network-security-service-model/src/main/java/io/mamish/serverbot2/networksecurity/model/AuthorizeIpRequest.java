package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

import java.net.Inet4Address;

@ApiRequestInfo(order = 2, name = "AuthorizeIp", numRequiredFields = 2,
        description = "Add inbound security group rules allowing this user IP, or replace the IP in their existing rules")
public class AuthorizeIpRequest {

    @ApiArgumentInfo(order = 0, name = "userId",
            description = "Nominally the Discord user ID. Used to tag security group rules for replacement.")
    String userId;

    @ApiArgumentInfo(order = 1, name = "userIpAddress", description = "IPv4 address of user to allow connections from")
    Inet4Address userIpAddress;

    public AuthorizeIpRequest() { }

    public AuthorizeIpRequest(String userId, Inet4Address userIpAddress) {
        this.userId = userId;
        this.userIpAddress = userIpAddress;
    }

    public String getUserId() {
        return userId;
    }

    public Inet4Address getUserIpAddress() {
        return userIpAddress;
    }
}
