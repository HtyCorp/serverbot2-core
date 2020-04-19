package io.mamish.serverbot2.sharedutil.reflect;

public class UnknownRequestException extends IllegalArgumentException {

    public UnknownRequestException(String message) {
        super(message);
    }

    public UnknownRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
