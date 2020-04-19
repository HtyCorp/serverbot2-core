package io.mamish.serverbot2.sharedutil.reflect;

public class RequestHandlingException extends RuntimeException {
    public RequestHandlingException(String message) {
        super(message);
    }

    public RequestHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
