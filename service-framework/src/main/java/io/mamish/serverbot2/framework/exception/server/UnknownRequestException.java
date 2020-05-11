package io.mamish.serverbot2.framework.exception.server;

public class UnknownRequestException extends ApiServerException {

    public UnknownRequestException(String message) {
        super(message);
    }

    public UnknownRequestException(String message, Throwable cause) {
        super(message, cause);
    }

}
