package io.mamish.serverbot2.workflow.model;

public interface IWorkflowService {

    SendMessageResponse runStepSendMessage(StartInstanceRequest request);
    StartInstanceResponse runStepStartInstance(StartInstanceRequest request);

}
