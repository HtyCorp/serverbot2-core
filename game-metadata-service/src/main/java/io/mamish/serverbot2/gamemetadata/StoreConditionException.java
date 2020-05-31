package io.mamish.serverbot2.gamemetadata;

public class StoreConditionException extends RuntimeException {

    public StoreConditionException(String message) {
        super(message);
    }

    public StoreConditionException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoreConditionException(Throwable cause) {
        super(cause);
    }

}
