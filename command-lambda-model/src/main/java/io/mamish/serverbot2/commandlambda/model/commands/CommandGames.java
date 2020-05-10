package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "games", numRequiredFields = 0,
        description = "List all games or get info on a game")
public class CommandGames extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, name = "game-name", description = "Name of game to look up")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
