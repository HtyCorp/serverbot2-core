package com.admiralbot.commandservice.commands;

import com.admiralbot.commandservice.model.ProcessUserCommandRequest;

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
