package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 5, name = "addguestip", numRequiredFields = 0,
        description = "Generates a reusable link to allow guests outside the Discord to temporarily whitelist their IPs")
public class CommandAddGuestIp extends AbstractCommandDto {
    // No params
}
