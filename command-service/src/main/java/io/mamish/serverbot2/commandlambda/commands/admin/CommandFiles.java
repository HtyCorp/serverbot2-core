package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 12, name = "files", numRequiredFields = 1,
        description = "View and edit files on a running game server")
public class CommandFiles extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game server to view/edit files of")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
