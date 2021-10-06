package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 5, name = "terminal", numRequiredFields = 1, description = "Get a terminal login link for a server")
public class CommandTerminal extends AbstractCommandDto  {

    @ApiArgumentInfo(order = 0, description = "Name of the game to connect to (must be running already)")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
