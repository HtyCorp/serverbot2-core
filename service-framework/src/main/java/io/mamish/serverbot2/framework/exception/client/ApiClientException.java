package io.mamish.serverbot2.framework.exception.client;

import io.mamish.serverbot2.framework.exception.ApiException;

public class ApiClientException extends ApiException {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
