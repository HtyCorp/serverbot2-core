package io.mamish.serverbot2.commandlambda.model.commands;

@Metadata.Command(docsPosition = 1, name = "games", numMinArguments = 0,
        description = "List all games or get info on a game")
public class CommandGames {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to look up")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
