package io.mamish.serverbot2.framework.exception;

public class UnknownRequestException extends ApiException {

    public UnknownRequestException() {
    }

    public UnknownRequestException(String message) {
        super(message);
    }

    public UnknownRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownRequestException(Throwable cause) {
        super(cause);
    }
}
