package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 9, name = "setdescription", numRequiredFields = 2,
        description = "Update the description for a game")
public class CommandSetDescription extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to update")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "New description for game")
    private String newDescription;

    public String getGameName() {
        return gameName;
    }

    public String getNewDescription() {
        return newDescription;
    }
}
