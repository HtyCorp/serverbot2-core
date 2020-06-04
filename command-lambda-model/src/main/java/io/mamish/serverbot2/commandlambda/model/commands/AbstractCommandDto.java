package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;

public abstract class AbstractCommandDto {

    private ProcessUserCommandRequest setContext;

    public void setContext(ProcessUserCommandRequest requestContext) {
        this.setContext = requestContext;
    }

    // This exists so command handlers can get the original user request (e.g. source sender and channel).
    // Would consider building a more general request stacking mechanism, if use by more components.
    public ProcessUserCommandRequest getContext() {
        return setContext;
    }

}
