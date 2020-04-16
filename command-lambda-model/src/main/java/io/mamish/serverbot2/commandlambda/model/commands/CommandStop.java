package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandStop {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to stop")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
