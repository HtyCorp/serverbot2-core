package io.mamish.serverbot2.framework.exception.server;

import io.mamish.serverbot2.framework.exception.ApiException;

public class ApiServerException extends ApiException {

    public ApiServerException(String message) {
        super(message);
    }

    public ApiServerException(String message, Throwable cause) {
        super(message, cause);
    }

}
