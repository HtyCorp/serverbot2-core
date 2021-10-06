package com.admiralbot.commandservice.commands.welcome;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 5, name = "ip", numRequiredFields = 0,
        description = "List the IP addresses of all running games or a specific game")
public class CommandIp extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to fetch IP address for")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
