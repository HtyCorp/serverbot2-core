package io.mamish.serverbot2.workflow;

import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.workflow.model.*;

public class LambdaHandler extends LambdaApiServer<IWorkflowService> implements IWorkflowService {

    @Override
    protected Class<IWorkflowService> getModelClass() {
        return IWorkflowService.class;
    }

    @Override
    protected IWorkflowService getHandlerInstance() {
        return this;
    }

    @Override
    public NewMessageResponse runStepNewMessage(NewMessageRequest request) {
        return null;
    }

    @Override
    public StartInstanceResponse runStepStartInstance(StartInstanceRequest request) {
        return null;
    }

}
