package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;

public interface Listener {

    UserCommandResponse onCommandHelp(CommandHelp commandHelp);
    UserCommandResponse onCommandGames(CommandGames commandGames);
    UserCommandResponse onCommandStart(CommandStart commandStart);
    UserCommandResponse onCommandStop(CommandStop commandStop);
    UserCommandResponse onCommandAddIp(CommandAddIp commandAddIp);

}
