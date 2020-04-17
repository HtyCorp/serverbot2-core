package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandDtoGames {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to look up")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
