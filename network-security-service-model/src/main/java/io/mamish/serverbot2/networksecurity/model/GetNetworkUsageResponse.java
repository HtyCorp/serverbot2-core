package io.mamish.serverbot2.networksecurity.model;

public class GetNetworkUsageResponse {

    private String earliestActivityTimeEpochMillis;
    private String latestActivityTimeEpochMillis;

    public GetNetworkUsageResponse() { }

    public GetNetworkUsageResponse(String earliestActivityTimeEpochMillis, String latestActivityTimeEpochMillis) {
        this.earliestActivityTimeEpochMillis = earliestActivityTimeEpochMillis;
        this.latestActivityTimeEpochMillis = latestActivityTimeEpochMillis;
    }

    public String getEarliestActivityTimeEpochMillis() {
        return earliestActivityTimeEpochMillis;
    }

    public String getLatestActivityTimeEpochMillis() {
        return latestActivityTimeEpochMillis;
    }
}
