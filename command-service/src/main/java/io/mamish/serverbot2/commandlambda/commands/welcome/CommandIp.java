package io.mamish.serverbot2.commandlambda.commands.welcome;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 5, name = "ip", numRequiredFields = 0,
        description = "List the IP addresses of all running games or a specific game")
public class CommandIp extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of game to fetch IP address for")
    private String gameName;

    public String getGameName() {
        return gameName;
    }
}
