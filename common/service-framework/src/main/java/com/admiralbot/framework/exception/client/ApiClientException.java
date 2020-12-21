package com.admiralbot.framework.exception.client;

import com.admiralbot.framework.exception.ApiException;

public class ApiClientException extends ApiException {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
