package io.mamish.serverbot2.framework.exception.client;

public class ApiClientIOException extends ApiClientException {

    public ApiClientIOException(String message) {
        super(message);
    }

    public ApiClientIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
