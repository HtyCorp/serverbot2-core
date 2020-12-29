package com.admiralbot.urlshortener.model;

import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "GetFullUrl", numRequiredFields = 2,
        description = "Get the full URL that was previously shortened by CreateShortUrl")
public class GetFullUrlRequest {

    // CRITICAL: If changing this, update URL shortener CF edge function since it relies on this model

    @ApiArgumentInfo(order = 0, description = "Token version provided by CreateShortUrl")
    private int tokenVersion;

    @ApiArgumentInfo(order = 1, description = "URL token provided by CreateShortUrl")
    private String urlToken;

    public GetFullUrlRequest() { }

    public GetFullUrlRequest(int tokenVersion, String urlToken) {
        this.tokenVersion = tokenVersion;
        this.urlToken = urlToken;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public String getUrlToken() {
        return urlToken;
    }

}
