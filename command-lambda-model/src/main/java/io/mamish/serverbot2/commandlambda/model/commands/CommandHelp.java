package io.mamish.serverbot2.commandlambda.model.commands;

@Metadata.Command(docsPosition = 0, name = "help", numMinArguments = 0,
        description = "Show help for a particular command"
)
public class CommandHelp {

    @Metadata.Argument(argPosition = 0, name = "command-name",
            description = "Name of command to look up")
    private String commandName;

    public String getCommandName() {
        return commandName;
    }
}
