package com.admiralbot.framework.exception.server;

public class RequestValidationException extends ApiServerException {

    public RequestValidationException(String message) {
        super(message);
    }

    public RequestValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
