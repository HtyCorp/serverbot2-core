package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 3, name = "openport", numRequiredFields = 2, description = "Open a firewall port on a game server")
public class CommandOpenPort extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to open firewall port for")
    private String gameName;

    @ApiArgumentInfo(order = 1, description = "Port range, e.g. 'udp:12345' or 'tcp:7000-8000'")
    private String portRange;

    public String getGameName() {
        return gameName;
    }

    public String getPortRange() {
        return portRange;
    }

}
