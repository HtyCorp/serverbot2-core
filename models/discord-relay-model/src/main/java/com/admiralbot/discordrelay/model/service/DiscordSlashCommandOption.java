package com.admiralbot.discordrelay.model.service;

import java.util.List;

public class DiscordSlashCommandOption {

    private DiscordSlashCommandOptionType type;
    private String name;
    private String description;
    private List<String> stringChoices;

    public DiscordSlashCommandOption() {}

    public DiscordSlashCommandOption(DiscordSlashCommandOptionType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public DiscordSlashCommandOption(DiscordSlashCommandOptionType type, String name, String description, List<String> stringChoices) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.stringChoices = stringChoices;
    }

    public DiscordSlashCommandOptionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getStringChoices() {
        return stringChoices;
    }
}
