package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "stop", numRequiredFields = 1,
        description = "Stop a running game")
public class CommandStop extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, name = "game-name", description = "Name of game to stop")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
