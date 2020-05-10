package io.mamish.serverbot2.framework.exception;

public class UnparsableInputException extends ApiException {

    public UnparsableInputException(String message) {
        super(message);
    }

    public UnparsableInputException(String message, Throwable cause) {
        super(message, cause);
    }

}
