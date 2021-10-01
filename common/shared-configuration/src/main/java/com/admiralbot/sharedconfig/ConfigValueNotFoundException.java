package com.admiralbot.sharedconfig;

public class ConfigValueNotFoundException extends RuntimeException {

    public ConfigValueNotFoundException(String message) {
        super(message);
    }

    public ConfigValueNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
