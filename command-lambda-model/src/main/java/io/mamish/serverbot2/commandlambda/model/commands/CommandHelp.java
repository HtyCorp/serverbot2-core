package io.mamish.serverbot2.commandlambda.model.commands;

public class CommandHelp {

    @Metadata.Argument(argPosition = 0, name = "command-name",
            description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }
}
