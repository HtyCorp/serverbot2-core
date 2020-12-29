package com.admiralbot.urlshortener.model;

public class GetFullUrlResponse {

    private String fullUrl;

    public GetFullUrlResponse() { }

    public GetFullUrlResponse(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getFullUrl() {
        return fullUrl;
    }

}
