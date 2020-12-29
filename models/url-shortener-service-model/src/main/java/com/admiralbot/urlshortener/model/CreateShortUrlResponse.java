package com.admiralbot.urlshortener.model;

public class CreateShortUrlResponse {

    private String shortUrl;

    public CreateShortUrlResponse() { }

    public CreateShortUrlResponse(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

}
