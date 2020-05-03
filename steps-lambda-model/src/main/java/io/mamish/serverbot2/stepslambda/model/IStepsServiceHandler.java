package io.mamish.serverbot2.stepslambda.model;

public interface IStepsServiceHandler {

    SendMessageStepResponse onStepSendMessage(StartInstanceStepRequest request);
    StartInstanceStepResponse onStepStartInstance(StartInstanceStepRequest request);

}
