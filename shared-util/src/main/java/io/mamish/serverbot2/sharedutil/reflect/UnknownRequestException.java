package io.mamish.serverbot2.sharedutil.reflect;

public class UnknownRequestException extends IllegalArgumentException {

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
