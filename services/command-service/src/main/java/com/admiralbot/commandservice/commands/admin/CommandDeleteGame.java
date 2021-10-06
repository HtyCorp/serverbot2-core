package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 2, name = "deletegame", numRequiredFields = 1, description = "Fully delete a game (use with caution!)")
public class CommandDeleteGame extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to delete")
    private String gameName;

    public CommandDeleteGame() { }

    public String getGameName() {
        return gameName;
    }
}
