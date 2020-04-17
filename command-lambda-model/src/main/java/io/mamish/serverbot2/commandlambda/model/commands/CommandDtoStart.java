package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandDtoStart {

    @Metadata.Argument(argPosition = 0, name = "game-name",
            description = "Name of game to start")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
