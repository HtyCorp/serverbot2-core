package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;

public interface ICommandHandler {
    
    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandGames(CommandGames commandGames);
    ProcessUserCommandResponse onCommandStart(CommandStart commandStart);
    ProcessUserCommandResponse onCommandStop(CommandStop commandStop);
    ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp);

}
