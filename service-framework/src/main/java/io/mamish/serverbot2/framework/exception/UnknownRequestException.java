package io.mamish.serverbot2.framework.exception;

public class UnknownRequestException extends ApiException {

    private String requestedTarget;

    public UnknownRequestException(String requestedTarget, String message) {
        super(message);
        this.requestedTarget = requestedTarget;
    }

    public UnknownRequestException(String requestedTarget, String message, Throwable cause) {
        super(message, cause);
        this.requestedTarget = requestedTarget;
    }

    public String getRequestedTarget() {
        return requestedTarget;
    }
}
