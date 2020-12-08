package io.mamish.serverbot2.commandlambda.commands.servers;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 5, name = "addguestip", numRequiredFields = 0,
        description = "Generates a reusable link to allow guests outside the Discord to temporarily whitelist their IPs")
public class CommandAddGuestIp extends AbstractCommandDto {
    // No params
}
