package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "newgame", numRequiredFields = 1,
        description = "Create a new blank game server (must install server software via terminal)")
public class CommandNewGame extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of new game")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
