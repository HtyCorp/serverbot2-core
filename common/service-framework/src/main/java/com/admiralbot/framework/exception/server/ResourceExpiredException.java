package com.admiralbot.framework.exception.server;

public class ResourceExpiredException extends ApiServerException {
    public ResourceExpiredException(String message) {
        super(message);
    }

    public ResourceExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
