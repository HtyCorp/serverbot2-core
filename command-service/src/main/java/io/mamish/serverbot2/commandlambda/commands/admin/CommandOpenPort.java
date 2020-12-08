package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

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
