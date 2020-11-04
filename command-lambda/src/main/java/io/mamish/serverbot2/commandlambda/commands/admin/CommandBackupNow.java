package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 6, name = "backupnow", numRequiredFields = 1,
        description = "Create a server backup right now")
public class CommandBackupNow extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to create backup for")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
