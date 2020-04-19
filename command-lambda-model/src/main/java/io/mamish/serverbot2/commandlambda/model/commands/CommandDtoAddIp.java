package io.mamish.serverbot2.commandlambda.model.commands;

import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

@ApiRequestInfo(order = 4, name = "addip", numRequiredFields = 0,
        description = "Add your IP address to allow connection")
public class CommandDtoAddIp extends AbstractCommandDto {}
