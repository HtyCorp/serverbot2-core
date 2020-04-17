package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandDtoHelp {

    @Metadata.Argument(argPosition = 0, name = "command-name",
            description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }
}
