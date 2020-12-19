package io.mamish.serverbot2.framework.exception.client;

public class ApiClientParseException extends ApiClientException {

    public ApiClientParseException(String message) {
        super(message);
    }

    public ApiClientParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
