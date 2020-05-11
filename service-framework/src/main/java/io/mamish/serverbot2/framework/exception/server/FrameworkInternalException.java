package io.mamish.serverbot2.framework.exception.server;

public class FrameworkInternalException extends ApiServerException {

    public FrameworkInternalException(String message) {
        super(message);
    }

    public FrameworkInternalException(String message, Throwable cause) {
        super(message, cause);
    }

}
