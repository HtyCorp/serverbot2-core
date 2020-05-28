package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.commands.*;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StateMachineListItem;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class CommandHandler implements ICommandHandler {

    private CommandDispatcher commandDispatcher;
    private Gson gson = new Gson();
    private Map<String,String> stateMachineArns;

    public CommandHandler() {
        commandDispatcher = new CommandDispatcher(this);
        stateMachineArns = SfnClient.create().listStateMachines().stateMachines().stream().collect(Collectors.toMap(
                StateMachineListItem::name,
                StateMachineListItem::stateMachineArn
        ));
    }

    public ProcessUserCommandResponse handleRequest(ProcessUserCommandRequest request) {
        return commandDispatcher.handleRequest(request);
    }

    @Override
    public ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            ApiActionDefinition definition = commandDispatcher.getApiDefinitionSet().getFromName(name);
            if (definition == null) {
                return new ProcessUserCommandResponse("Can't look up help: '" + name + "' is not a recognised command name.");
            } else {
                StringBuilder detailedHelpBuilder = new StringBuilder();
                detailedHelpBuilder.append(definition.getUsageString());
                detailedHelpBuilder.append("\n  ").append(definition.getDescription());
                for (String argString: definition.getArgumentDescriptionStrings()) {
                    detailedHelpBuilder.append("\n    ").append(argString);
                }
                return new ProcessUserCommandResponse(detailedHelpBuilder.toString());
            }
        } else {
            String aggregateHelpString = commandDispatcher.getApiDefinitionSet().getAll().stream()
                    .map(definition -> definition.getUsageString() + "\n  " + definition.getDescription())
                    .collect(Collectors.joining("\n"));
            return new ProcessUserCommandResponse(aggregateHelpString);
        }
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
