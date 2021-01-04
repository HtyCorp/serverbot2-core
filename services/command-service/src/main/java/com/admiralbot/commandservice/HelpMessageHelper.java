package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.common.CommandHelp;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.framework.common.ApiActionDefinition;
import com.admiralbot.framework.common.ApiDefinitionSet;

import java.util.stream.Collectors;

public class HelpMessageHelper {

    private final ApiDefinitionSet<?> definitionSet;

    public HelpMessageHelper(ApiDefinitionSet<?> definitionSet) {
        this.definitionSet = definitionSet;
    }

    public boolean hasDefinitionFor(String commandName) {
        return definitionSet.getFromName(commandName) != null;
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
