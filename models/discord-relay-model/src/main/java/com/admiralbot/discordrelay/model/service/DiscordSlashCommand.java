package com.admiralbot.discordrelay.model.service;

import java.util.List;

public class DiscordSlashCommand {

    private MessageChannel permissionLevel;
    private String name;
    private String description;
    private int numRequiredOptions;
    private List<DiscordSlashCommandOption> options;

    public DiscordSlashCommand() {}

    public DiscordSlashCommand(MessageChannel permissionLevel, String name, String description, int numRequiredOptions,
                               List<DiscordSlashCommandOption> options) {
        this.permissionLevel = permissionLevel;
        this.name = name;
        this.description = description;
        this.numRequiredOptions = numRequiredOptions;
        this.options = options;
    }

    public MessageChannel getPermissionLevel() {
        return permissionLevel;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getNumRequiredOptions() {
        return numRequiredOptions;
    }

    public List<DiscordSlashCommandOption> getOptions() {
        return options;
    }

}
