package com.admiralbot.commandservice.commands.welcome;

import com.admiralbot.commandservice.commands.common.CommandHelp;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.framework.common.FrameworkApiModel;

@FrameworkApiModel
public interface IWelcomeCommandHandler {

    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandJoin(CommandJoin commandJoin);
    ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave);
    ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp);
    ProcessUserCommandResponse onCommandIp(CommandIp commandIp);

}
