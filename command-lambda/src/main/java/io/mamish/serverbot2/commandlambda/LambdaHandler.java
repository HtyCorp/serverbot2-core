package io.mamish.serverbot2.commandlambda;

import com.amazonaws.xray.AWSXRay;
import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;

public class LambdaHandler extends LambdaApiServer<ICommandService> {

    @Override
    protected Class<ICommandService> getModelClass() {
        return ICommandService.class;
    }

    @Override
    protected ICommandService createHandlerInstance() {
        try {
            AWSXRay.beginSubsegment("BuildRootHandler");
            return new RootCommandHandler();
        } catch (RuntimeException e) {
            AWSXRay.getCurrentSubsegment().addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

}
