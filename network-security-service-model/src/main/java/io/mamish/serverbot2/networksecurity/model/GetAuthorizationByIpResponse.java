package io.mamish.serverbot2.networksecurity.model;

public class GetAuthorizationByIpResponse {

    private boolean isAuthorized;
    private long expiryTimeEpochSeconds;

    public GetAuthorizationByIpResponse() { }

    public GetAuthorizationByIpResponse(boolean isAuthorized, long expiryTimeEpochSeconds) {
        this.isAuthorized = isAuthorized;
        this.expiryTimeEpochSeconds = expiryTimeEpochSeconds;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public long getExpiryTimeEpochSeconds() {
        return expiryTimeEpochSeconds;
    }

}
