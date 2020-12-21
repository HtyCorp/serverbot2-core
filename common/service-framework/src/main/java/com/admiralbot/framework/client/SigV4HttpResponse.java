package com.admiralbot.framework.client;

import java.util.Optional;

public class SigV4HttpResponse {

    private final int statusCode;
    private final String statusText;
    private final Optional<String> body;

    public SigV4HttpResponse(int statusCode, String statusText, Optional<String> body) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public Optional<String> getBody() {
        return body;
    }
}
