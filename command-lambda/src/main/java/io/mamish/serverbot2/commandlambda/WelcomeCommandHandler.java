package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandJoin;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandLeave;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.IWelcomeCommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.discordrelay.model.service.ModifyRoleMembershipRequest;
import io.mamish.serverbot2.discordrelay.model.service.RoleModifyOperation;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedutil.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiFunction;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private final Logger logger = LogManager.getLogger(WelcomeCommandHandler.class);

    private final IDiscordService discordServiceClient;

    public WelcomeCommandHandler() {
        logger.trace("Building DiscordRelay client");
        discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
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
        return runChange(commandJoin.getContext().getSenderId(), commandJoin.getChannel(), RoleModifyOperation.ADD_USER,
                (user, channel) -> "Added <@" + user + "> to " + channel + " channel",
                (user, channel) -> "Couldn't add you to " + channel + " channel role. It might already be assigned to you.");
    }

    @Override
    public ProcessUserCommandResponse onCommandLeave(CommandLeave commandLeave) {
        return runChange(commandLeave.getContext().getSenderId(), commandLeave.getChannel(), RoleModifyOperation.REMOVE_USER,
                (user, channel) -> "Removed <@" + user + "> from " + channel + " channel",
                (user, channel) -> "Couldn't remove you from " + channel + " channel role. It might not be assigned to you.");
    };

    private ProcessUserCommandResponse runChange(String userId, String channelName, RoleModifyOperation operation,
                                                 BiFunction<String,String,String> userChannelMessageSuccess,
                                                 BiFunction<String,String,String> userChannelMessageFailure) {

        String errMsg = "Channel to join must be either 'servers' or 'debug'";
        MessageChannel channel;
        try {
            channel = MessageChannel.valueOf(channelName.toUpperCase());
            if (!Utils.equalsAny(channel, MessageChannel.SERVERS, MessageChannel.DEBUG)) {
                throw new RequestValidationException(errMsg);
            }
        } catch (IllegalArgumentException e) {
            throw new RequestValidationException(errMsg);
        }

        try {
            discordServiceClient.modifyRoleMembership(new ModifyRoleMembershipRequest(userId, channel, operation));
        } catch (ApiServerException e) {
            logger.error("Failed to run " + operation + " for user " + userId + " on " + channelName, e);
            return new ProcessUserCommandResponse(userChannelMessageFailure.apply(userId, channelName));
        }

        logger.info("Successfully ran " + operation + " for user " + userId + " on " + channelName);
        return new ProcessUserCommandResponse(userChannelMessageSuccess.apply(userId, channelName));

    }

}
