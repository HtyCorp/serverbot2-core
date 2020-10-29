package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.common.CommandHelp;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.common.ApiDefinitionSet;

import java.util.stream.Collectors;

public class HelpHandler {

    private final ApiDefinitionSet<?> definitionSet;

    public HelpHandler(ApiDefinitionSet<?> definitionSet) {
        this.definitionSet = definitionSet;
    }

    public ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp) {

        if (commandHelp.getCommandName() != null) {
            String name = commandHelp.getCommandName();
            ApiActionDefinition definition = definitionSet.getFromName(name);
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
            String aggregateHelpString = definitionSet.getAll().stream()
                    .map(definition -> definition.getUsageString() + "\n  " + definition.getDescription())
                    .collect(Collectors.joining("\n"));
            return new ProcessUserCommandResponse(aggregateHelpString);
        }

    }


}
