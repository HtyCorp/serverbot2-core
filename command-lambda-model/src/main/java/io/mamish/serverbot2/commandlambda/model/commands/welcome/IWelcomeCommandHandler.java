package io.mamish.serverbot2.commandlambda.model.commands.welcome;

import io.mamish.serverbot2.commandlambda.model.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;

public interface IWelcomeCommandHandler {

    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandJoin(CommandJoin commandJoin);
    ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave);

}
