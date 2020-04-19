package io.mamish.serverbot2.commandlambda.model.service;

public interface ICommandServiceHandler {
    CommandServiceResponse onRequestUserCommand(CommandServiceRequest commandServiceRequest);
}
