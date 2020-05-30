package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandGames;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStart;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStop;
import io.mamish.serverbot2.commandlambda.model.commands.servers.IServersCommandHandler;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandAddIp;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.server.AbstractApiRequestDispatcher;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StateMachineListItem;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class ServersCommandHandler extends AbstractCommandHandler<IServersCommandHandler> implements IServersCommandHandler {

    private final Gson gson = new Gson();
    private final SfnClient sfnClient;
    private final Map<String,String> stateMachineArns;

    public ServersCommandHandler(SfnClient sfnClient, Map<String,String> stateMachineArns) {
        this.sfnClient = sfnClient;
        this.stateMachineArns = stateMachineArns;
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

    private String getStateMachineArn(String name) throws NoSuchElementException {
        return stateMachineArns.get(name);
    }

}
