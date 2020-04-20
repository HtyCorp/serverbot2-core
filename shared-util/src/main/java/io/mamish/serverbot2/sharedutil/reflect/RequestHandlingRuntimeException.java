package io.mamish.serverbot2.sharedutil.reflect;

public class RequestHandlingRuntimeException extends RuntimeException {
    public RequestHandlingRuntimeException(String message) {
        super(message);
    }

    public RequestHandlingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
