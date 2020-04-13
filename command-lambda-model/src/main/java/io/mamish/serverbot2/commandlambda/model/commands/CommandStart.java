package io.mamish.serverbot2.commandlambda.model.commands;

@Metadata.Command(docsPosition = 2, name = "start", numMinArguments = 1,
        description = "Start a game")
public class CommandStart {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to start")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
