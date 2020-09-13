package io.mamish.serverbot2.framework.exception.server;

public class ResourceAlreadyExistsException extends ApiServerException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
