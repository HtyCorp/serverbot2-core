package io.mamish.serverbot2.workflow.model;

public interface IWorkflowService {

    NewMessageResponse runStepNewMessage(NewMessageRequest request);
    NewInstanceResponse runStepNewInstance(NewInstanceRequest request);
    StartInstanceResponse runStepStartInstance(StartInstanceRequest request);

}
