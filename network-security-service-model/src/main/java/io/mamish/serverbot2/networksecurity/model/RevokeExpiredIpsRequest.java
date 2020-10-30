package io.mamish.serverbot2.networksecurity.model;

import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 8, name = "RevokeExpiredIps", numRequiredFields = 0,
        description = "Revokes all IP authorizations that are past their expiry time. Intended to be called periodically by ResourceReaper.")
public class RevokeExpiredIpsRequest {
    // No params
}
