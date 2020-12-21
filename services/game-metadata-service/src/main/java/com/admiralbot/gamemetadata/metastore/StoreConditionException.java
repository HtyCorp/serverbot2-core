package com.admiralbot.gamemetadata.metastore;

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
