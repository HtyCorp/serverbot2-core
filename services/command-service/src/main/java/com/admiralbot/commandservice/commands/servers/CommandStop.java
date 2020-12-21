package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "stop", numRequiredFields = 1,
        description = "Stop a running game")
public class CommandStop extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to stop")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
