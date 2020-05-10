package io.mamish.serverbot2.framework.exception;

public class RequestHandlingRuntimeException extends ApiException {
    public RequestHandlingRuntimeException(String message) {
        super(message);
    }

    public RequestHandlingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestHandlingRuntimeException(Throwable cause) {
        super(cause);
    }

    public RequestHandlingRuntimeException() {
    }
}
