package io.mamish.serverbot2.framework.exception.client;

public class ApiRequestTimeoutException extends ApiClientException {

    public ApiRequestTimeoutException(String message) {
        super(message);
    }

    public ApiRequestTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
