package com.admiralbot.urlshortener.userprefs;

import software.amazon.awssdk.core.SdkBytes;

import java.util.Base64;

public class WebPushSubscription {

    private static final Base64.Encoder b64UrlEncoder = Base64.getUrlEncoder();

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

    public String getKeyBase64UrlEncoded() {
        return b64UrlEncoder.encodeToString(keyBytes.asByteArray());
    }

    public SdkBytes getAuthBytes() {
        return authBytes;
    }

    public String getAuthBase64UrlEncoded() {
        return b64UrlEncoder.encodeToString(authBytes.asByteArray());
    }

}
