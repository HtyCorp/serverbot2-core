package io.mamish.serverbot2.framework.exception;

public class RequestValidationException extends ApiException {
    public RequestValidationException(String message) {
        super(message);
    }

    public RequestValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestValidationException(Throwable cause) {
        super(cause);
    }

    public RequestValidationException() {
    }
}
