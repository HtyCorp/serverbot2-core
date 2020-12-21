package com.admiralbot.framework.exception.server;

public class UnparsableInputException extends ApiServerException {

    public UnparsableInputException(String message) {
        super(message);
    }

    public UnparsableInputException(String message, Throwable cause) {
        super(message, cause);
    }

}
