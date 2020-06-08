package io.mamish.serverbot2.commandlambda.model;

public interface ICommandService {
    ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandServiceRequest);
}
