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
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiFunction;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private final Logger logger = LogManager.getLogger(WelcomeCommandHandler.class);

    private final IDiscordService discordServiceClient;
    private final INetworkSecurity networkSecurityClient;
    private final UrlShortenerClient urlShortenerClient;

    public WelcomeCommandHandler() {
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

}
