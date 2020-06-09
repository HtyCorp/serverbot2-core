package io.mamish.serverbot2.commandlambda.commands.admin;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "terminal", numRequiredFields = 1, description = "Get a terminal login link for a server")
public class CommandTerminal extends AbstractCommandDto  {

    @ApiArgumentInfo(order = 0, description = "Name of the game to connect to (must be running already)")
    private String gameName;

    public CommandTerminal() { }

    public CommandTerminal(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }

}
