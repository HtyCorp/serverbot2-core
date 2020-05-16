package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "GenerateIpAuthUrl", numRequiredFields = 1,
        description = "Generate a presigned URL a user can visit to authorise their IP for server access")
public class GenerateIpAuthUrlRequest {

    @ApiArgumentInfo(order = 0, description = "Nominally the Discord user ID. Used to tag security group rules for replacement.")
    private String userId;

    public GenerateIpAuthUrlRequest() {}

    public GenerateIpAuthUrlRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
