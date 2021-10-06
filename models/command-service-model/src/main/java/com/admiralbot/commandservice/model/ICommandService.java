package com.admiralbot.commandservice.model;

import com.admiralbot.framework.modelling.ApiAuthType;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "commandservice", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface ICommandService {
    ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest request);
    GenerateSlashCommandSetResponse generateSlashCommandSet(GenerateSlashCommandSetRequest request);
}
