package io.mamish.serverbot2.commandlambda.model.service;

public interface ICommandService {
    CommandServiceResponse requestUserCommand(CommandServiceRequest commandServiceRequest);
}
