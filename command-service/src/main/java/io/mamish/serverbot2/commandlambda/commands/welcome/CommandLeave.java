package io.mamish.serverbot2.commandlambda.commands.welcome;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "leave", numRequiredFields = 0,
        description = "Leave the main server bot channel - this hides it from your view to reduce message spam")
public class CommandLeave extends AbstractCommandDto {

    // No arguments: previously allowed a channel choice, but as the entry command this should be as simple as possible.

}
