package com.admiralbot.networksecurity.firewall;

import com.google.gson.annotations.SerializedName;

public class DiscordUserAuthInfo {

    // Uses shorter serialised name since space is limited in PL entry description field

    @SerializedName("v")
    private long schemaVersion;

    @SerializedName("r")
    private String reservationId;

    @SerializedName("t")
    private long authTimeEpochSeconds;

    @SerializedName("a")
    private DiscordUserAuthType authType;

    @SerializedName("u")
    private String userId;

    public DiscordUserAuthInfo() {}

    public DiscordUserAuthInfo(long schemaVersion, String reservationId, long authTimeEpochSeconds, DiscordUserAuthType authType, String userId) {
        this.schemaVersion = schemaVersion;
        this.reservationId = reservationId;
        this.authTimeEpochSeconds = authTimeEpochSeconds;
        this.authType = authType;
        this.userId = userId;
    }

    public long getSchemaVersion() {
        return schemaVersion;
    }

    public String getReservationId() {
        return reservationId;
    }

    public long getAuthTimeEpochSeconds() {
        return authTimeEpochSeconds;
    }

    public DiscordUserAuthType getAuthType() {
        return authType;
    }

    public String getUserId() {
        return userId;
    }
}
