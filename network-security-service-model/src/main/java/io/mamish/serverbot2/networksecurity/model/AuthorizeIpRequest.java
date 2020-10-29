package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "AuthorizeIp", numRequiredFields = 1,
        description = "Add inbound security group rules allowing this user IP, or replace the IP in their existing rules")
public class AuthorizeIpRequest {

    @ApiArgumentInfo(order = 0, description = "IPv4 address of user to allow connections from")
    String userIpAddress;

    @ApiArgumentInfo(order = 1, description = "The encrypted user ID generated as part of a GenerateIpAuthUrl request")
    String encryptedUserId;

    @ApiArgumentInfo(order = 2, description = "Nominally the Discord user ID. Used to tag security group rules for replacement")
    String userId;

    public AuthorizeIpRequest() { }

    public AuthorizeIpRequest(String userIpAddress, String encryptedUserId, String userId) {
        this.userIpAddress = userIpAddress;
        this.encryptedUserId = encryptedUserId;
        this.userId = userId;
    }

    public String getUserIpAddress() {
        return userIpAddress;
    }

    public String getEncryptedUserId() {
        return encryptedUserId;
    }

    public String getUserId() {
        return userId;
    }
}
