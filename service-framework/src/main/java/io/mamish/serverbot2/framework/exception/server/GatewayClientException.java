package io.mamish.serverbot2.framework.exception.server;

public class GatewayClientException extends ApiServerException {

    public GatewayClientException(String message) {
        super(message);
    }

    public GatewayClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
