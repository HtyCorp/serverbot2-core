package com.admiralbot.networksecurity.model;

public class GetNetworkUsageResponse {

    private boolean hasAnyActivity;
    private int latestActivityAgeSeconds;

    public GetNetworkUsageResponse() { }

    public GetNetworkUsageResponse(boolean hasAnyActivity, int latestActivityAgeSeconds) {
        this.hasAnyActivity = hasAnyActivity;
        this.latestActivityAgeSeconds = latestActivityAgeSeconds;
    }

    public boolean hasAnyActivity() {
        return hasAnyActivity;
    }

    public int getLatestActivityAgeSeconds() {
        return latestActivityAgeSeconds;
    }

}
