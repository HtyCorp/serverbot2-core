package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "start", numRequiredFields = 1,
        description = "Start a game")
public class CommandStart extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to start")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
