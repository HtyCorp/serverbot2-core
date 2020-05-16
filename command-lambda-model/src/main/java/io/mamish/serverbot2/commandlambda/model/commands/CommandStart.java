package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "start", numRequiredFields = 1,
        description = "Start a game")
public class CommandStart extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to start")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
