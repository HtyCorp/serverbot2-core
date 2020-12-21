package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 8, name = "RevokeExpiredIps", numRequiredFields = 0,
        description = "Revokes all IP authorizations that are past their expiry time. Intended to be called periodically by ResourceReaper.")
public class RevokeExpiredIpsRequest {
    // No params
}
