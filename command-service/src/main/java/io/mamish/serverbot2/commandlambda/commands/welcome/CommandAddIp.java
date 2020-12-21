package io.mamish.serverbot2.commandlambda.commands.welcome;


import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "addip", numRequiredFields = 0,
        description = "Whitelist your IP address to join servers - you can run this command from any server bot channel")
public class CommandAddIp extends AbstractCommandDto {
    // No params
}
