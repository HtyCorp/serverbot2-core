package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "start", numRequiredFields = 1,
        description = "Start a game")
public class CommandStart extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, name = "game-name", description = "Name of game to start")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
