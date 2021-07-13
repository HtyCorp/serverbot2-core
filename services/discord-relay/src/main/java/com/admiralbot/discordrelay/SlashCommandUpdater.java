package com.admiralbot.discordrelay;

import com.admiralbot.discordrelay.model.service.DiscordSlashCommand;
import com.admiralbot.discordrelay.model.service.DiscordSlashCommandOption;
import com.admiralbot.discordrelay.model.service.DiscordSlashCommandOptionType;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.sharedconfig.DiscordConfig;
import com.admiralbot.sharedutil.Utils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SlashCommandUpdater {

    private final DiscordApi discordApi;
    private final ChannelMap channelMap;

    public SlashCommandUpdater(DiscordApi discordApi, ChannelMap channelMap) {
        this.discordApi = discordApi;
        this.channelMap = channelMap;
    }

    public void putSlashCommands(List<DiscordSlashCommand> requestedCommands) {

        Server primaryServer = channelMap.getPrimaryServer();

        // Push the requested commands and save the response to get the resulting IDs
        List<SlashCommandBuilder> definitions = Utils.map(requestedCommands, this::buildSlashCommandDefinition);
        List<SlashCommand> resultCommands = discordApi.bulkOverwriteServerSlashCommands(primaryServer, definitions).join();

        // Update the permissions on all commands; we need the IDs from above to refer to them
        Map<String,Long> nameToId = resultCommands.stream().collect(Collectors.toMap(SlashCommand::getName, SlashCommand::getId));
        List<ServerSlashCommandPermissionsBuilder> permissions = Utils.map(requestedCommands,
                command -> buildSlashCommandPermissions(nameToId.get(command.getName()), command));
        discordApi.batchUpdateSlashCommandPermissions(primaryServer, permissions);
    }

    private SlashCommandBuilder buildSlashCommandDefinition(DiscordSlashCommand command) {

        // Build a simple command with name and description; by default it has no permissions (not accessible by anyone)
        SlashCommandBuilder commandBuilder = new SlashCommandBuilder()
                .setName(command.getName())
                .setDescription(command.getDescription())
                .setDefaultPermission(false);

        // Add options (i.e. arguments/parameters) to command, if given
        if (command.getOptions() != null) {
            for (int optionIndex = 0; optionIndex < command.getOptions().size(); optionIndex++) {
                DiscordSlashCommandOption option = command.getOptions().get(optionIndex);

                SlashCommandOptionBuilder optionBuilder = new SlashCommandOptionBuilder()
                        .setType(convertOptionType(option.getType()))
                        .setName(option.getName())
                        .setDescription(option.getDescription())
                        .setRequired(optionIndex < command.getNumRequiredOptions());

                // Add choices (i.e. enum input) to this option, if given
                if (option.getStringChoices() != null) {
                    option.getStringChoices().forEach(choice -> optionBuilder.addChoice(choice, choice));
                }

                commandBuilder.addOption(optionBuilder.build());
            }
        }

        return commandBuilder;
    }

    private ServerSlashCommandPermissionsBuilder buildSlashCommandPermissions(long commandId, DiscordSlashCommand command) {
        List<SlashCommandPermissions> permissions = new ArrayList<>();

        // Reminder: permission order is ADMIN -> MAIN -> WELCOME
        // Always grant permission to ADMIN
        permissions.add(makeRoleCommandPermission(DiscordConfig.CHANNEL_ROLE_ADMIN.getValue()));
        // If level isn't ADMIN, it's either MAIN or WELCOME: grant to MAIN
        if (command.getPermissionLevel() != MessageChannel.ADMIN) {
            permissions.add(makeRoleCommandPermission(DiscordConfig.CHANNEL_ROLE_MAIN.getValue()));
            // If level still isn't main, it's WELCOME: grant to everyone (note guild ID doubles as '@everyone' role)
            if (command.getPermissionLevel() != MessageChannel.MAIN) {
                permissions.add(makeRoleCommandPermission(channelMap.getPrimaryServer().getIdAsString()));
            }
        }

        return new ServerSlashCommandPermissionsBuilder(commandId, permissions);
    }

    private SlashCommandPermissions makeRoleCommandPermission(String roleId) {
        return SlashCommandPermissions.create(Long.parseLong(roleId), SlashCommandPermissionType.ROLE, true);
    }

    private SlashCommandOptionType convertOptionType(DiscordSlashCommandOptionType type) {
        switch(type) {
            case BOOLEAN: return SlashCommandOptionType.BOOLEAN;
            case INTEGER: return SlashCommandOptionType.INTEGER;
            case STRING: return SlashCommandOptionType.STRING;
            default: throw new IllegalStateException("Unexpected option type: " + type);
        }
    }
}
