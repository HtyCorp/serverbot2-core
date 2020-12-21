package com.admiralbot.framework.exception.server;

public class UnknownRequestException extends ApiServerException {

    private String requestedTargetName;

    public UnknownRequestException(String message) {
        super(message);
    }

    public UnknownRequestException(String message, String requestedTargetName) {
        super(message);
        this.requestedTargetName = requestedTargetName;
    }

    public UnknownRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getRequestedTargetName() {
        return requestedTargetName;
    }
}
