package io.mamish.serverbot2.framework.exception.server;

public class RequestHandlingException extends ApiServerException {

    public RequestHandlingException(String message) {
        super(message);
    }

    public RequestHandlingException(String message, Throwable cause) {
        super(message, cause);
    }

}
