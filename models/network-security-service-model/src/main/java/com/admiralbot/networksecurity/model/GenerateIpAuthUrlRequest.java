package com.admiralbot.networksecurity.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "GenerateIpAuthUrl", numRequiredFields = 1,
        description = "Generate a presigned URL a user can visit to authorise their IP for server access")
public class GenerateIpAuthUrlRequest {

    @ApiArgumentInfo(order = 0, description = "Opaque ID to indicate source (typically Discord command message ID)")
    private String reservationId;

    @ApiArgumentInfo(order = 1, description = "Discord user ID to associate with. Can be null for guest auth.")
    private String userId;

    public GenerateIpAuthUrlRequest() {}

    public GenerateIpAuthUrlRequest(String reservationId, String userId) {
        this.reservationId = reservationId;
        this.userId = userId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }
}
