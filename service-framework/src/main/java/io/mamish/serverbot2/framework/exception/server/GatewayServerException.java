package io.mamish.serverbot2.framework.exception.server;

public class GatewayServerException extends ApiServerException{

    public GatewayServerException(String message) {
        super(message);
    }

    public GatewayServerException(String message, Throwable cause) {
        super(message, cause);
    }

}
