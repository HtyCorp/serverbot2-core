package io.mamish.serverbot2.framework.exception;

public class FrameworkInternalException extends ApiException {

    public FrameworkInternalException() {
    }

    public FrameworkInternalException(String message) {
        super(message);
    }

    public FrameworkInternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrameworkInternalException(Throwable cause) {
        super(cause);
    }
}
