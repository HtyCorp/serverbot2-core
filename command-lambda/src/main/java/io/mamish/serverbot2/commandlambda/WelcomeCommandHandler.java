package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.welcome.CommandAddIp;
import io.mamish.serverbot2.commandlambda.commands.welcome.CommandJoin;
import io.mamish.serverbot2.commandlambda.commands.welcome.CommandLeave;
import io.mamish.serverbot2.commandlambda.commands.welcome.IWelcomeCommandHandler;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.*;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.networksecurity.model.GenerateIpAuthUrlRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiFunction;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private final Logger logger = LogManager.getLogger(WelcomeCommandHandler.class);

    private final IDiscordService discordServiceClient;
    private final INetworkSecurity networkSecurityClient;

    public WelcomeCommandHandler() {
        logger.trace("Building DiscordRelay client");
        discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
        logger.trace("Building NetworkSecurity client");
        networkSecurityClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
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
        String userId = commandAddIp.getContext().getSenderId();
        String authUrl = networkSecurityClient.generateIpAuthUrl(
                new GenerateIpAuthUrlRequest(userId)
        ).getIpAuthUrl();

        // Send a message to the user privately before returning the standard channel message.

        String welcomeMessage = "Thanks for using serverbot2. To whitelist your IP to join servers, click the following link:\n\n";
        String urlParagraph = authUrl + "\n\n";
        String reassurance = "This will detect your IP and add it to the firewall. If you've done this before, it replaces your last IP.\n\n";
        String why = "(You're seeing this message because you sent an 'addip' message (ID "
                + commandAddIp.getContext().getMessageId() + ") in the serverbot2 '"
                + commandAddIp.getContext().getChannel().toLowerCase() + "' channel. Exposing these servers publicly "
                + "is a security risk I'm responsible for, so only whitelisted IPs are allowed from now on.)";

        String messageContent = welcomeMessage + urlParagraph + reassurance + why;

        discordServiceClient.newMessage(new NewMessageRequest(
                messageContent,
                null,
                null,
                userId
        ));

        return new ProcessUserCommandResponse(
                "A whitelist link has been sent to your private messages."
        );
    }

}
