package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;

public interface IAdminCommandHandler {

    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandNewGame(CommandNewGame commandNewGame);
    ProcessUserCommandResponse onCommandOpenPort(CommandOpenPort commandOpenPort);
    ProcessUserCommandResponse onCommandClosePort(CommandClosePort commandClosePort);

}
