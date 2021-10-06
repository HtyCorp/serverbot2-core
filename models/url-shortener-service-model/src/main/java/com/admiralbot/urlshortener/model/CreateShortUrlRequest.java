package com.admiralbot.urlshortener.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "CreateShortUrl", numRequiredFields = 2,
        description = "Create a shortened URL that can be fetched to get a longer URL")
public class CreateShortUrlRequest {

    @ApiArgumentInfo(order = 0, description = "Full URL that shortened URL will map to")
    private String fullUrl;

    @ApiArgumentInfo(order = 1, description = "Time in seconds the short URL will be valid for")
    private long ttlSeconds;

    public CreateShortUrlRequest() { }

    public CreateShortUrlRequest(String fullUrl, long ttlSeconds) {
        this.fullUrl = fullUrl;
        this.ttlSeconds = ttlSeconds;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

}
