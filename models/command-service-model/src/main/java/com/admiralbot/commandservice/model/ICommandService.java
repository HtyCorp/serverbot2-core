package com.admiralbot.commandservice.model;

import com.admiralbot.framework.common.ApiAuthType;
import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "commandservice", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface ICommandService {
    ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest request);
    GenerateSlashCommandSetResponse generateSlashCommandSet(GenerateSlashCommandSetRequest request);
}
