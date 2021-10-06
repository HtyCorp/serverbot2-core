package com.admiralbot.discordrelay.model.service;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

import java.util.List;

@ApiRequestInfo(order = 3, name = "PutSlashCommands", numRequiredFields = 1,
        description = "Creates or updates Discord slash commands for the guild")
public class PutSlashCommandsRequest {

    @ApiArgumentInfo(order = 0, description = "The slash commands to add to the guild")
    private List<DiscordSlashCommand> slashCommands;

    public PutSlashCommandsRequest() {}

    public PutSlashCommandsRequest(List<DiscordSlashCommand> slashCommands) {
        this.slashCommands = slashCommands;
    }

    public List<DiscordSlashCommand> getSlashCommands() {
        return slashCommands;
    }
}
