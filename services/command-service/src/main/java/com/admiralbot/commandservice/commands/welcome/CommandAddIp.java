package com.admiralbot.commandservice.commands.welcome;


import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "addip", numRequiredFields = 0,
        description = "Whitelist your IP address to join servers - you can run this command from any server bot channel")
public class CommandAddIp extends AbstractCommandDto {
    // No params
}
