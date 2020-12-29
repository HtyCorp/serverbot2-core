package com.admiralbot.urlshortener.model;

public class GetFullUrlResponse {

    // CRITICAL: If changing this, update URL shortener CF edge function since it relies on this model

    private String fullUrl;

    public GetFullUrlResponse() { }

    public GetFullUrlResponse(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getFullUrl() {
        return fullUrl;
    }

}
