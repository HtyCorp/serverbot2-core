package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.welcome.*;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.*;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.gamemetadata.model.GameMetadata;
import io.mamish.serverbot2.gamemetadata.model.GameReadyState;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.gamemetadata.model.ListGamesRequest;
import io.mamish.serverbot2.networksecurity.model.GenerateIpAuthUrlRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private final Logger logger = LogManager.getLogger(WelcomeCommandHandler.class);

    private final IGameMetadataService gameMetadataServiceClient;
    private final IDiscordService discordServiceClient;
    private final INetworkSecurity networkSecurityClient;
    private final UrlShortenerClient urlShortenerClient;

    private final Ec2Client ec2Client = Ec2Client.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(new SystemSettingsRegionProvider().getRegion())
            .build();

    public WelcomeCommandHandler() {
        logger.trace("Builder GameMetadataService client");
        gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);
        logger.trace("Building DiscordRelay client");
        discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
        logger.trace("Building NetworkSecurity client");
        networkSecurityClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
        logger.trace("Building UrlShortenerClient");
        urlShortenerClient = new UrlShortenerClient();
        logger.trace("Constructor finished");
    }

    @Override
    protected Class<IWelcomeCommandHandler> getHandlerType() {
        return IWelcomeCommandHandler.class;
    }

    @Override
    protected IWelcomeCommandHandler getHandlerInstance() {
        return this;
    }

    @Override
    public ProcessUserCommandResponse onCommandJoin(CommandJoin commandJoin) {
        MessageChannel channelToJoin = MessageChannel.SERVERS; // Deliberately not user-selectable for UX reasons
        return changeRole(commandJoin.getContext().getSenderId(), channelToJoin, RoleModifyOperation.ADD_USER,
                (user, channel) -> "Added <@" + user + "> to " + channel + " channel",
                (user, channel) -> "Couldn't add you to " + channel + " channel role. It might already be assigned to you.");
    }

    @Override
    public ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave) {
        MessageChannel channelToLeave = MessageChannel.SERVERS; // Deliberately not user-selectable for UX reasons
        return changeRole(commandLeave.getContext().getSenderId(), channelToLeave, RoleModifyOperation.REMOVE_USER,
                (user, channel) -> "Removed <@" + user + "> from " + channel + " channel",
                (user, channel) -> "Couldn't remove you from " + channel + " channel role. It might not be assigned to you.");
    }

    private ProcessUserCommandResponse changeRole(String userId, MessageChannel channel, RoleModifyOperation operation,
                                                  BiFunction<String,String,String> userChannelMessageSuccess,
                                                  BiFunction<String,String,String> userChannelMessageFailure) {

        String channelName = channel.toLowerCase();

        try {
            discordServiceClient.modifyRoleMembership(new ModifyRoleMembershipRequest(userId, channel, operation));
        } catch (ApiServerException e) {
            logger.error("Failed to run " + operation + " for user " + userId + " on " + channelName, e);
            return new ProcessUserCommandResponse(userChannelMessageFailure.apply(userId, channelName));
        }

        logger.info("Successfully ran " + operation + " for user " + userId + " on " + channelName);
        return new ProcessUserCommandResponse(userChannelMessageSuccess.apply(userId, channelName));

    }

    @Override
    public ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp) {
        String fullAuthUrl = networkSecurityClient.generateIpAuthUrl(
                new GenerateIpAuthUrlRequest(commandAddIp.getContext().getSenderId())
        ).getIpAuthUrl();

        String shortAuthUrl = urlShortenerClient.getShortenedUrl(fullAuthUrl, NetSecConfig.AUTH_URL_TTL.getSeconds());

        // Send a message to the user privately before returning the standard channel message.

        String friendlyDomain = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();

        String summaryMessage = "To whitelist your IP to join **"+friendlyDomain+"** servers, use this link.\n";
        String detailMessage = "This will detect your IP and add it to the firewall. If you've done this before, it"
                + " replaces your last whitelisted IP.\n";
        String reassureMessage = "For any questions, message Mamish#7674 or view this bot's code at"
                + " https://github.com/HtyCorp/serverbot2-core";

        String messageContent = summaryMessage + detailMessage + reassureMessage;

        SimpleEmbed authLinkEmbed = new SimpleEmbed(
                shortAuthUrl,
                "Whitelist IP for " + commandAddIp.getContext().getSenderName(),
                "Personal link to detect and whitelist your IP address for " + friendlyDomain
        );

        discordServiceClient.newMessage(new NewMessageRequest(
                messageContent,
                null,
                null,
                commandAddIp.getContext().getSenderId(),
                authLinkEmbed
        ));

        return new ProcessUserCommandResponse(
                "A whitelist link has been sent to your private messages."
        );
    }

    @Override
    public ProcessUserCommandResponse onCommandIp(CommandIp commandIp) {

        List<GameMetadata> gameMetadataList = gameMetadataServiceClient.listGames(new ListGamesRequest()).getGames();

        Map<String,String> runningInstanceIdsToGameNames = gameMetadataList.stream()
                .filter(game -> game.getInstanceId() != null)
                .filter(game -> Utils.equalsAny(game.getGameReadyState(), GameReadyState.STARTING, GameReadyState.RUNNING))
                .collect(Collectors.toMap(GameMetadata::getInstanceId, GameMetadata::getGameName));

        if (runningInstanceIdsToGameNames.isEmpty()) {
            return new ProcessUserCommandResponse("There are no games running right now.");
        }

        Map<String,String> runningGameNamesToIps = ec2Client.describeInstancesPaginator(r -> r.instanceIds(runningInstanceIdsToGameNames.keySet()))
                .reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.toMap(i -> runningInstanceIdsToGameNames.get(i.instanceId()), Instance::publicIpAddress));

        StringBuilder outputBuilder = new StringBuilder();
        runningGameNamesToIps.forEach((name, ip) -> {
            outputBuilder.append(name).append(": ");
            if (ip == null) {
                outputBuilder.append("server is still starting up");
            } else {
                String dnsName = IDUtils.dot(name, CommonConfig.APP_ROOT_DOMAIN_NAME.getValue());
                outputBuilder.append(ip).append(" (").append(dnsName).append(")");
            }
            outputBuilder.append("\n");
        });

        outputBuilder.append("\nNote: your IP address must be whitelisted (use !addip) to connect to these.");

        return new ProcessUserCommandResponse(outputBuilder.toString());

    }
}
