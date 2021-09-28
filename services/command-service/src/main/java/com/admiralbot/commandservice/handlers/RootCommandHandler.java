package com.admiralbot.commandservice.handlers;

import com.admiralbot.commandservice.AbstractCommandHandler;
import com.admiralbot.commandservice.IpAuthMessageHelper;
import com.admiralbot.commandservice.model.*;
import com.admiralbot.discordrelay.model.service.*;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.RequestHandlingRuntimeException;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.sharedutil.Utils;
import com.admiralbot.urlshortener.model.IUrlShortener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RootCommandHandler implements ICommandService {

    private final Logger logger = LoggerFactory.getLogger(RootCommandHandler.class);

    private final AdminCommandHandler adminCommandHandler;
    private final ServersCommandHandler serversCommandHandler;
    private final WelcomeCommandHandler welcomeCommandHandler;

    public RootCommandHandler() {

        logger.trace("Initialising GameMetadata client");
        IGameMetadataService gameMetadataServiceClient = ApiClient.http(IGameMetadataService.class);
        logger.trace("Initialising NetworkSecurity client");
        INetworkSecurity networkSecurityServiceClient = ApiClient.http(INetworkSecurity.class);
        logger.trace("Initialising DiscordRelay client");
        IDiscordService discordServiceClient = ApiClient.http(IDiscordService.class);
        logger.trace("Initialising URLShortener client");
        IUrlShortener urlShortenerClient = ApiClient.http(IUrlShortener.class);

        IpAuthMessageHelper ipAuthMessageHelper = new IpAuthMessageHelper(networkSecurityServiceClient, urlShortenerClient);

        logger.trace("Initialising EC2 client");
        Ec2Client ec2Client = SdkUtils.client(Ec2Client.builder());

        logger.trace("Creating admin command handler...");
        adminCommandHandler = new AdminCommandHandler(ec2Client, gameMetadataServiceClient,
                networkSecurityServiceClient, urlShortenerClient);
        logger.trace("Creating servers command handler...");
        serversCommandHandler = new ServersCommandHandler(gameMetadataServiceClient, ipAuthMessageHelper);
        logger.trace("Creating welcome command handler...");
        welcomeCommandHandler = new WelcomeCommandHandler(ec2Client, gameMetadataServiceClient, discordServiceClient,
                ipAuthMessageHelper);

        logger.trace("Chaining handlers...");
        // Chaining: admin -> debug (pending) -> servers -> welcome
        // Commands from lower in chain can be used implicitly from a higher-level channel.
        adminCommandHandler.setNextChainHandler(serversCommandHandler);
        serversCommandHandler.setNextChainHandler(welcomeCommandHandler);
        logger.trace("RootCommandHandler init finished");

    }

    @Override
    public ProcessUserCommandResponse processUserCommand(ProcessUserCommandRequest commandRequest) {
        // Passes the request to the appropriate handler based on source channel.
        // Note that the chaining mechanism will pass requests downwards if not found in source channel.
        MessageChannel channel = commandRequest.getChannel();
        if (channel == MessageChannel.ADMIN) {
            return adminCommandHandler.handleRequest(commandRequest);
        } else if (channel == MessageChannel.MAIN) {
            return serversCommandHandler.handleRequest(commandRequest);
        } else if (channel == MessageChannel.WELCOME) {
            return welcomeCommandHandler.handleRequest(commandRequest);
        } else {
            throw new RequestHandlingRuntimeException("Impossible: received an unknown channel");
        }
    }

    @Override
    public GenerateSlashCommandSetResponse generateSlashCommandSet(GenerateSlashCommandSetRequest request) {
        List<DiscordSlashCommand> slashCommands = new ArrayList<>();
        slashCommands.addAll(generateSlashCommands(adminCommandHandler, MessageChannel.ADMIN, true));
        slashCommands.addAll(generateSlashCommands(serversCommandHandler, MessageChannel.MAIN, true));
        slashCommands.addAll(generateSlashCommands(welcomeCommandHandler, MessageChannel.WELCOME, false));
        return new GenerateSlashCommandSetResponse(slashCommands);
    }

    private List<DiscordSlashCommand> generateSlashCommands(AbstractCommandHandler<?> handler,
                                                            MessageChannel permissionLevel, boolean ignoreHelp) {
        return handler.getCommandDefinitionSet().getAll().stream()
                .filter(command -> !(ignoreHelp && command.getName().equals("help")))
                .map(command -> {
                    List<DiscordSlashCommandOption> commandOptions = Utils.map(command.getOrderedFields(),
                            fieldAndInfo -> new DiscordSlashCommandOption(
                                    convertFieldTypeToCommandOption(fieldAndInfo.a().getType()),
                                    fieldAndInfo.a().getName(),
                                    fieldAndInfo.b().description(),
                                    null // Choices aren't supported just yet
                            ));
                    return new DiscordSlashCommand(permissionLevel, command.getName(), command.getDescription(),
                            command.getNumRequiredFields(), commandOptions);
                }).collect(Collectors.toList());
    }

    private DiscordSlashCommandOptionType convertFieldTypeToCommandOption(Class<?> fieldType) {
        if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
            return DiscordSlashCommandOptionType.BOOLEAN;
        } else if (fieldType == Integer.class || fieldType == Integer.TYPE) {
            return DiscordSlashCommandOptionType.INTEGER;
        } else if (fieldType == String.class) {
            return DiscordSlashCommandOptionType.STRING;
        } else {
            throw new IllegalStateException("Unsupported option type: " + fieldType);
        }
    }
}
