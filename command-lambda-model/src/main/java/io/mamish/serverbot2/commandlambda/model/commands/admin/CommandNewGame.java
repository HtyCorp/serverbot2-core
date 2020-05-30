package io.mamish.serverbot2.commandlambda.model.commands.admin;

import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "newgame", numRequiredFields = 1, description = "Create a new blank game server")
public class CommandNewGame {

    @ApiArgumentInfo(order = 0, description = "Name of new game")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
