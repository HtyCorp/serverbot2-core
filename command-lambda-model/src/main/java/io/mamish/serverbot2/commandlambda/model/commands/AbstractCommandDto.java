package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;

public abstract class AbstractCommandDto {

    private CommandServiceRequest originalRequest;

    public void setOriginalRequest(CommandServiceRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    protected CommandServiceRequest getOriginalRequest() {
        return originalRequest;
    }

}
