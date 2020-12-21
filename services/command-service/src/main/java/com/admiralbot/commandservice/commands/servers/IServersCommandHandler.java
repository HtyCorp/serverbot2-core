package com.admiralbot.commandservice.commands.servers;

import com.admiralbot.commandservice.commands.common.CommandHelp;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;

public interface IServersCommandHandler {
    
    ProcessUserCommandResponse onCommandHelp(CommandHelp commandHelp);
    ProcessUserCommandResponse onCommandGames(CommandGames commandGames);
    ProcessUserCommandResponse onCommandStart(CommandStart commandStart);
    ProcessUserCommandResponse onCommandStop(CommandStop commandStop);
    ProcessUserCommandResponse onCommandAddGuestIp(CommandAddGuestIp commandAddGuestIp);

}
