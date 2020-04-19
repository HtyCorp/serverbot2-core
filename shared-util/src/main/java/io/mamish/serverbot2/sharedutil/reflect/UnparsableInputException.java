package io.mamish.serverbot2.sharedutil.reflect;

public class UnparsableInputException extends IllegalArgumentException {

    public UnparsableInputException(String message) {
        super(message);
    }

    public UnparsableInputException(String message, Throwable cause) {
        super(message, cause);
    }

}
