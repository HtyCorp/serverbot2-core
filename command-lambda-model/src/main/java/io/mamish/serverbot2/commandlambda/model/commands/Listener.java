package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;

public interface Listener {

    @Metadata.Command(docsPosition = 0, name = "help", numMinArguments = 0,
            description = "Show help for a particular command")
    UserCommandResponse onCommandHelp(CommandHelp commandHelp);

    @Metadata.Command(docsPosition = 1, name = "games", numMinArguments = 0,
            description = "List all games or get info on a game")
    UserCommandResponse onCommandGames(CommandGames commandGames);

    @Metadata.Command(docsPosition = 2, name = "start", numMinArguments = 1,
            description = "Start a game")
    UserCommandResponse onCommandStart(CommandStart commandStart);

    @Metadata.Command(docsPosition = 3, name = "stop", numMinArguments = 1,
            description = "Stop a running game")
    UserCommandResponse onCommandStop(CommandStop commandStop);

    @Metadata.Command(docsPosition = 4, name = "addip", numMinArguments = 0,
            description = "Add your IP address to allow connection")
    UserCommandResponse onCommandAddIp(CommandAddIp commandAddIp);

}
