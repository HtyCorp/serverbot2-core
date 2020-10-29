package io.mamish.serverbot2.commandlambda.commands.servers;

import io.mamish.serverbot2.commandlambda.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;

public interface IServersCommandHandler {
    
    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandGames(CommandGames commandGames);
    ProcessUserCommandResponse onCommandStart(CommandStart commandStart);
    ProcessUserCommandResponse onCommandStop(CommandStop commandStop);

}
