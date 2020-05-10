package io.mamish.serverbot2.framework.exception;

public class RequestHandlingException extends ApiException {
    public RequestHandlingException(String message) {
        super(message);
    }

    public RequestHandlingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestHandlingException(Throwable cause) {
        super(cause);
    }

    public RequestHandlingException() {
    }
}
