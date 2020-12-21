package com.admiralbot.commandservice.commands.common;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiArgumentInfo;
import com.admiralbot.framework.common.ApiRequestInfo;

// This is referenced by all channel-specific model interfaces because they each run a different version of 'help',
// i.e. they all give output about their own specific commands.
@ApiRequestInfo(order = 0, name = "help", numRequiredFields = 0,
        description = "Show help for a particular command")
public class CommandHelp extends AbstractCommandDto {

    @ApiArgumentInfo(order = 0, description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }

}
