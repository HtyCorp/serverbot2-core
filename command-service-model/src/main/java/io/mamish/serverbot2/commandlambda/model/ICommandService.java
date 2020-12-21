package io.mamish.serverbot2.commandlambda.model;

import io.mamish.serverbot2.framework.common.ApiAuthType;
import io.mamish.serverbot2.framework.common.ApiEndpointInfo;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;

@ApiEndpointInfo(serviceName = "commandservice", uriPath = "/", httpMethod = ApiHttpMethod.POST, authType = ApiAuthType.IAM)
public interface ICommandService {
    ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandServiceRequest);
}
