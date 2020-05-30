package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandJoin;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandLeave;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.IWelcomeCommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;

public class WelcomeCommandHandler extends AbstractCommandHandler<IWelcomeCommandHandler> implements IWelcomeCommandHandler {

    private IDiscordService discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);

    @Override
    protected Class<IWelcomeCommandHandler> getHandlerType() {
        return IWelcomeCommandHandler.class;
    }

    @Override
    protected IWelcomeCommandHandler getHandlerInstance() {
        return this;
    }

    @Override
    public ProcessUserCommandResponse onCommandJoin(CommandJoin command) {
        return null;
    }

    @Override
    public ProcessUserCommandResponse onCommandLeave(CommandLeave command) {
        return null;
    }
}
