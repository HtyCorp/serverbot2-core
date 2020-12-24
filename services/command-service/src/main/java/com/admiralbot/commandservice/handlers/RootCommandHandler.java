package com.admiralbot.commandservice.handlers;

import com.admiralbot.commandservice.IpAuthMessageHelper;
import com.admiralbot.commandservice.UrlShortenerClient;
import com.admiralbot.commandservice.model.ICommandService;
import com.admiralbot.commandservice.model.ProcessUserCommandRequest;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.RequestHandlingRuntimeException;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.sharedutil.SdkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RootCommandHandler implements ICommandService {

    private final Logger logger = LogManager.getLogger(RootCommandHandler.class);

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
        UrlShortenerClient urlShortenerClient = new UrlShortenerClient();

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
}
