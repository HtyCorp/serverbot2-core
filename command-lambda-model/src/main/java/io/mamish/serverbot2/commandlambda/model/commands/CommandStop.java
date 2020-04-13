package io.mamish.serverbot2.commandlambda.model.commands;

@Metadata.Command(docsPosition = 3, name = "stop", numMinArguments = 1,
        description = "Stop a running game")
public class CommandStop {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to stop")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
