package com.admiralbot.commandservice.commands.admin;

import com.admiralbot.commandservice.commands.common.CommandHelp;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;

public interface IAdminCommandHandler {

    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandNewGame(CommandNewGame commandNewGame);
    ProcessUserCommandResponse onCommandSetDescription(CommandSetDescription commandSetDescription);
    ProcessUserCommandResponse onCommandDeleteGame(CommandDeleteGame commandDeleteGame);
    ProcessUserCommandResponse onCommandOpenPort(CommandOpenPort commandOpenPort);
    ProcessUserCommandResponse onCommandClosePort(CommandClosePort commandClosePort);
    ProcessUserCommandResponse onCommandTerminal(CommandTerminal commandTerminal);
    ProcessUserCommandResponse onCommandBackupNow(CommandBackupNow commandBackupNow);
    ProcessUserCommandResponse onCommandFiles(CommandFiles commandFiles);
    ProcessUserCommandResponse onCommandExtendDisk(CommandExtendDisk commandExtendDisk);

}
