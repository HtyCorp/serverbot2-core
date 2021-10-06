package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 6, name = "backupnow", numRequiredFields = 1,
        description = "Create a server backup right now")
public class CommandBackupNow extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to create backup for")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
