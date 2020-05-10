package io.mamish.serverbot2.framework.exception;

public class RequestValidationException extends ApiException {
    public RequestValidationException(String message) {
        super(message);
    }
}
