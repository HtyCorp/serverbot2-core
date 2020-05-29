package io.mamish.serverbot2.commandlambda.model.commands.common;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiArgumentInfo;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "help", numRequiredFields = 0,
        description = "Show help for a particular command")
public class CommandHelp extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }

}
