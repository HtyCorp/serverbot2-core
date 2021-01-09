package com.admiralbot.urlshortener.userprefs;

import software.amazon.awssdk.core.SdkBytes;

public class WebPushSubscription {

    private String pushEndpoint;
    private SdkBytes keyBytes;
    private SdkBytes authBytes;

    public WebPushSubscription() { }

    public WebPushSubscription(String pushEndpoint, SdkBytes keyBytes, SdkBytes authBytes) {
        this.pushEndpoint = pushEndpoint;
        this.keyBytes = keyBytes;
        this.authBytes = authBytes;
    }

    public String getPushEndpoint() {
        return pushEndpoint;
    }

    public SdkBytes getKeyBytes() {
        return keyBytes;
    }

    public SdkBytes getAuthBytes() {
        return authBytes;
    }
}
