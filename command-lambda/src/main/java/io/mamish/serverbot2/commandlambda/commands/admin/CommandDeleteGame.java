package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "DeleteGame", numRequiredFields = 1, description = "Fully delete a game (use with caution!)")
public class CommandDeleteGame extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to delete")
    private String gameName;

    public CommandDeleteGame() { }

    public String getGameName() {
        return gameName;
    }
}
