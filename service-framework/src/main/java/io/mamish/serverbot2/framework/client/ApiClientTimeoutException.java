package io.mamish.serverbot2.framework.client;

public class ApiClientTimeoutException extends RuntimeException {

    public ApiClientTimeoutException() {
        super();
    }

    public ApiClientTimeoutException(String message) {
        super(message);
    }

    public ApiClientTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiClientTimeoutException(Throwable cause) {
        super(cause);
    }

}
