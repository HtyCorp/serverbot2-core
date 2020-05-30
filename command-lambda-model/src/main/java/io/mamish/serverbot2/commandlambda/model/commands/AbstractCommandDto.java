package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;

public abstract class AbstractCommandDto {

    private ProcessUserCommandRequest originalRequest;

    public void setOriginalRequest(ProcessUserCommandRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    // This exists so command handlers can get the original user request (e.g. source sender and channel).
    // Would consider building a more general request stacking mechanism, if use by more components.
    public ProcessUserCommandRequest getOriginalRequest() {
        return originalRequest;
    }

}
