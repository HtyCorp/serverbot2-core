package io.mamish.serverbot2.sharedutil.reflect;

public class RequestValidationException extends IllegalArgumentException {
    public RequestValidationException(String message) {
        super(message);
    }
}
