package io.mamish.serverbot2.commandlambda.commands.welcome;

import io.mamish.serverbot2.commandlambda.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;

public interface IWelcomeCommandHandler {

    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandJoin(CommandJoin commandJoin);
    ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave);
    ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp);

}
