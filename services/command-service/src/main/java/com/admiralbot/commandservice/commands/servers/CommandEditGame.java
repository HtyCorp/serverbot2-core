package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 14, name = "editgame", numRequiredFields = 1,
        description = "Start a game's host without running the actual game server")
public class CommandEditGame extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to edit")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
