package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandDtoStop {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to stop")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
