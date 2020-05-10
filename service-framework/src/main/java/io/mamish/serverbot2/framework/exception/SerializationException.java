package io.mamish.serverbot2.framework.exception;

public class SerializationException extends ApiException {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

    public SerializationException() {
    }
}
