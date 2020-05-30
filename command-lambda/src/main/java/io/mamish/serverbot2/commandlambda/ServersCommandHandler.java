package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandGames;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStart;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStop;
import io.mamish.serverbot2.commandlambda.model.commands.servers.IServersCommandHandler;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandAddIp;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import software.amazon.awssdk.services.sfn.SfnClient;

import java.util.Map;
import java.util.NoSuchElementException;

public class ServersCommandHandler extends AbstractCommandHandler<IServersCommandHandler> implements IServersCommandHandler {

    private final Gson gson = new Gson();
    private final SfnRunner sfnRunner;

    public ServersCommandHandler(SfnRunner sfnRunner) {
        this.sfnRunner = sfnRunner;
    }

    @Override
    protected Class<IServersCommandHandler> getHandlerType() {
        return IServersCommandHandler.class;
    }

    @Override
    protected IServersCommandHandler getHandlerInstance() {
        return this;
    }

    @Override
    public ProcessUserCommandResponse onCommandGames(CommandGames commandGames) {
        return new ProcessUserCommandResponse("Echo 'games': " + gson.toJson(commandGames));
    }

    @Override
    public ProcessUserCommandResponse onCommandStart(CommandStart commandStart) {
        return new ProcessUserCommandResponse("Echo 'start': " + gson.toJson(commandStart));
    }

    @Override
    public ProcessUserCommandResponse onCommandStop(CommandStop commandStop) {
        return new ProcessUserCommandResponse("Echo 'stop': " + gson.toJson(commandStop));
    }

    @Override
    public ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp) {
        return new ProcessUserCommandResponse("Echo 'addip': " + gson.toJson(commandAddIp));
    }

}
