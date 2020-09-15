package io.mamish.serverbot2.framework.exception.server;

public class ServiceLimitException extends ApiServerException {

    public ServiceLimitException(String message) {
        super(message);
    }

    public ServiceLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
