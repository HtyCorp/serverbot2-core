package com.admiralbot.commandservice.commands.welcome;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "leave", numRequiredFields = 0,
        description = "Leave the main server bot channel - this hides it from your view to reduce message spam")
public class CommandLeave extends AbstractCommandDto {

    // No arguments: previously allowed a channel choice, but as the entry command this should be as simple as possible.

}
