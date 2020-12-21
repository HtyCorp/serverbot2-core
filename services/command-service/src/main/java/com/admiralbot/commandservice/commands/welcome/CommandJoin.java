package com.admiralbot.commandservice.commands.welcome;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 0, name = "join", numRequiredFields = 0,
        description = "Join the main server bot channel (normally hidden to reduce spam) so you can interact with it")
public class CommandJoin extends AbstractCommandDto {

    // No arguments: previously allowed a channel choice, but as the entry command this should be as simple as possible.

}
