package com.admiralbot.framework.exception.client;

public class ApiClientParseException extends ApiClientException {

    public ApiClientParseException(String message) {
        super(message);
    }

    public ApiClientParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
