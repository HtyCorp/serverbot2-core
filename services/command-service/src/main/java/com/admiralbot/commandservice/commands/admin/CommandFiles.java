package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(order = 12, name = "files", numRequiredFields = 1,
        description = "View and edit files on a running game server")
public class CommandFiles extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game server to view/edit files of")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
