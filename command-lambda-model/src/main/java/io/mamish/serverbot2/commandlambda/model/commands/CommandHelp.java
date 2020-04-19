package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "help", numRequiredFields = 0,
        description = "Show help for a particular command")
public class CommandHelp extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, name = "command-name", description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }

}
