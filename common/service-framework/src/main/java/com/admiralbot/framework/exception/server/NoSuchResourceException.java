package com.admiralbot.framework.exception.server;

public class NoSuchResourceException extends ApiServerException {

    public NoSuchResourceException(String message) {
        super(message);
    }

    public NoSuchResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
