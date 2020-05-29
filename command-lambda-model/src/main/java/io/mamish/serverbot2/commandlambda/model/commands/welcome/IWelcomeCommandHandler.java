package io.mamish.serverbot2.commandlambda.model.commands.welcome;

import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;

public interface IWelcomeCommandHandler {

    ProcessUserCommandResponse onCommandJoin(CommandJoin command);
    ProcessUserCommandResponse onCommandLeave(CommandLeave command);

}
