package io.mamish.serverbot2.stepslambda.model;

public interface IStepsService {

    SendMessageStepResponse runStepSendMessage(StartInstanceStepRequest request);
    StartInstanceStepResponse runStepStartInstance(StartInstanceStepRequest request);

}
