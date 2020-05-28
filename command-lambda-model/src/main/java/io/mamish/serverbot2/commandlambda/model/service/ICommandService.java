package io.mamish.serverbot2.commandlambda.model.service;

public interface ICommandService {
    ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandServiceRequest);
}
