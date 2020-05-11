package io.mamish.serverbot2.framework.exception.server;

public class RequestHandlingRuntimeException extends ApiServerException {

    public RequestHandlingRuntimeException(String message) {
        super(message);
    }

    public RequestHandlingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
