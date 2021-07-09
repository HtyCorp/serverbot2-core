package com.admiralbot.commandservice.model;

import com.admiralbot.discordrelay.model.service.DiscordSlashCommand;

import java.util.List;

public class GenerateSlashCommandSetResponse {

    private List<DiscordSlashCommand> slashCommands;

    public GenerateSlashCommandSetResponse() {}

    public GenerateSlashCommandSetResponse(List<DiscordSlashCommand> slashCommands) {
        this.slashCommands = slashCommands;
    }

    public List<DiscordSlashCommand> getSlashCommands() {
        return slashCommands;
    }
}
