package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "games", numRequiredFields = 0,
        description = "List all games or get info on a game")
public class CommandGames extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to look up")
    private String gameName;

    public String getGameName() {
        return gameName;
    }

}
