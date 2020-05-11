package io.mamish.serverbot2.framework.exception.server;

public class SerializationException extends ApiServerException {

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
