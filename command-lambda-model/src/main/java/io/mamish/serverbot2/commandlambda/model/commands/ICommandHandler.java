package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;

public interface ICommandHandler {
    
    CommandServiceResponse onCommandHelp(CommandHelp commandHelp);
    CommandServiceResponse onCommandGames(CommandGames commandGames);
    CommandServiceResponse onCommandStart(CommandStart commandStart);
    CommandServiceResponse onCommandStop(CommandStop commandStop);
    CommandServiceResponse onCommandAddIp(CommandDtoAddIp commandAddIp);

}
