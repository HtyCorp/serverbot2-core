package com.admiralbot.framework.exception.server;

import com.admiralbot.framework.exception.ApiException;

public class ApiServerException extends ApiException {

    public ApiServerException(String message) {
        super(message);
    }

    public ApiServerException(String message, Throwable cause) {
        super(message, cause);
    }

}
