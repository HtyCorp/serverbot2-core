package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.ICommandService;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingRuntimeException;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RootCommandHandler implements ICommandService {

    private final Logger logger = LogManager.getLogger(RootCommandHandler.class);

    private final AdminCommandHandler adminCommandHandler;
    private final ServersCommandHandler serversCommandHandler;
    private final WelcomeCommandHandler welcomeCommandHandler;

    public RootCommandHandler() {

        logger.trace("Initialising GameMetadata client");
        IGameMetadataService gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);
        logger.trace("Initialising NetworkSecurity client");
        INetworkSecurity networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
        logger.trace("Initialising DiscordRelay client");
        IDiscordService discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
        logger.trace("Initialising URLShortener client");
        UrlShortenerClient urlShortenerClient = new UrlShortenerClient();

        IpAuthMessageHelper ipAuthMessageHelper = new IpAuthMessageHelper(discordServiceClient,
                networkSecurityServiceClient, urlShortenerClient);

        logger.trace("Creating admin command handler...");
        adminCommandHandler = new AdminCommandHandler(gameMetadataServiceClient, networkSecurityServiceClient,
                discordServiceClient, urlShortenerClient);
        logger.trace("Creating servers command handler...");
        serversCommandHandler = new ServersCommandHandler(gameMetadataServiceClient, ipAuthMessageHelper);
        logger.trace("Creating welcome command handler...");
        welcomeCommandHandler = new WelcomeCommandHandler(gameMetadataServiceClient, discordServiceClient,
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
        } else if (channel == MessageChannel.DEBUG) {
            // There are no commands yet, and Discord relay is configured not to relay from this channel.
            return new ProcessUserCommandResponse(null, null);
        } else if (channel == MessageChannel.SERVERS) {
            return serversCommandHandler.handleRequest(commandRequest);
        } else if (channel == MessageChannel.WELCOME) {
            return welcomeCommandHandler.handleRequest(commandRequest);
        } else {
            throw new RequestHandlingRuntimeException("Impossible: received an unknown channel");
        }
    }
}
