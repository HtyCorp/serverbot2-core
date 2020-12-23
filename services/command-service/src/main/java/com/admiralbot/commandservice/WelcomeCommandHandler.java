package com.admiralbot.commandservice;

import com.admiralbot.commandservice.commands.welcome.*;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.discordrelay.model.service.ModifyRoleMembershipRequest;
import com.admiralbot.discordrelay.model.service.RoleModifyOperation;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.gamemetadata.model.GameMetadata;
import com.admiralbot.gamemetadata.model.GameReadyState;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.gamemetadata.model.ListGamesRequest;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private final Logger logger = LogManager.getLogger(WelcomeCommandHandler.class);

    private final Ec2Client ec2Client;
    private final IGameMetadataService gameMetadataServiceClient;
    private final IDiscordService discordServiceClient;
    private final IpAuthMessageHelper ipAuthMessageHelper;

    public WelcomeCommandHandler(Ec2Client ec2Client, IGameMetadataService gameMetadataServiceClient,
                                 IDiscordService discordServiceClient, IpAuthMessageHelper ipAuthMessageHelper) {
        this.ec2Client = ec2Client;
        this.gameMetadataServiceClient = gameMetadataServiceClient;
        this.discordServiceClient = discordServiceClient;
        this.ipAuthMessageHelper = ipAuthMessageHelper;
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
        MessageChannel channelToJoin = MessageChannel.MAIN; // Deliberately not user-selectable for UX reasons
        return changeRole(commandJoin.getContext().getSenderId(), channelToJoin, RoleModifyOperation.ADD_USER,
                (user, channel) -> "Added <@" + user + "> to " + channel + " channel",
                (user, channel) -> "Couldn't add you to " + channel + " channel role. It might already be assigned to you.");
    }

    @Override
    public ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave) {
        MessageChannel channelToLeave = MessageChannel.MAIN; // Deliberately not user-selectable for UX reasons
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
        return ipAuthMessageHelper.handleMemberIpAuthRequest(commandAddIp);
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
                String dnsName = Joiner.dot(name, CommonConfig.APP_ROOT_DOMAIN_NAME.getValue());
                outputBuilder.append(ip).append(" (").append(dnsName).append(")");
            }
            outputBuilder.append("\n");
        });

        outputBuilder.append("\nNote: your IP address must be whitelisted (use !addip) to connect to these.");

        return new ProcessUserCommandResponse(outputBuilder.toString());

    }
}
