package io.mamish.serverbot2.sharedutil.reflect;

public class SerializationException extends IllegalArgumentException {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
