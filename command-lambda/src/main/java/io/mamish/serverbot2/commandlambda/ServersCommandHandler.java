package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandGames;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStart;
import io.mamish.serverbot2.commandlambda.model.commands.servers.CommandStop;
import io.mamish.serverbot2.commandlambda.model.commands.servers.IServersCommandHandler;
import io.mamish.serverbot2.commandlambda.model.commands.welcome.CommandAddIp;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.networksecurity.model.GenerateIpAuthUrlRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import io.mamish.serverbot2.workflow.model.Machines;

import java.util.List;

public class ServersCommandHandler extends AbstractCommandHandler<IServersCommandHandler> implements IServersCommandHandler {

    private final IGameMetadataService gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class,
            GameMetadataConfig.FUNCTION_NAME);
    private final INetworkSecurity networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class,
            NetSecConfig.FUNCTION_NAME);
    private final IDiscordService discordServiceClient = ApiClient.sqs(IDiscordService.class,
            DiscordConfig.SQS_QUEUE_NAME);

    private final SfnRunner sfnRunner = new SfnRunner();

    public ServersCommandHandler() { }

    @Override
    protected Class<IServersCommandHandler> getHandlerType() {
        return IServersCommandHandler.class;
    }

    @Override
    protected IServersCommandHandler getHandlerInstance() {
        return this;
    }

    @Override
    public ProcessUserCommandResponse onCommandGames(CommandGames commandGames) {
        if (commandGames.getGameName() != null) {
            return runShowSpecificGame(commandGames.getGameName());
        } else {
            return runShowAllGames();
        }
    }

    private ProcessUserCommandResponse runShowSpecificGame(String name) {
        DescribeGameResponse response = gameMetadataServiceClient.describeGame(new DescribeGameRequest(name));
        if (response.isPresent()) {
            GameMetadata game = response.getGame();
            StringBuilder output = new StringBuilder();
            output.append("Game ID: ").append(game.getGameName()).append(" (use this in other commands)\n");
            output.append("Game description: ").append(game.getFullName()).append("\n");
            output.append("Current status: ").append(game.getGameReadyState().toLowerCase());
            return new ProcessUserCommandResponse(output.toString());
        } else {
            throw makeUnknownGameException(name);
        }
    }

    private ProcessUserCommandResponse runShowAllGames() {
        List<GameMetadata> games = gameMetadataServiceClient.listGames(new ListGamesRequest()).getGames();
        final StringBuilder output = new StringBuilder();
        if (games.isEmpty()) {
            output.append("No games available yet.");
        } else {
            output.append("Available games (").append(games.size()).append("):\n");
            games.forEach(game -> {
                output.append(game.getGameName()).append(" (").append(game.getFullName()).append(" )");
                output.append(", currently ").append(game.getGameReadyState().toLowerCase()).append("\n");
            });
        }
        return new ProcessUserCommandResponse(output.toString());
    }

    @Override
    public ProcessUserCommandResponse onCommandStart(CommandStart commandStart) {
        String name = commandStart.getGameName();
        DescribeGameResponse describe = gameMetadataServiceClient.describeGame(new DescribeGameRequest(name));
        if (describe.isPresent()) {
            GameMetadata game = describe.getGame();
            if (game.getGameReadyState() != GameReadyState.STOPPED) {
                throw new RequestHandlingException(name + " isn't in a valid state to start (" +
                        game.getGameReadyState().toLowerCase() + ").");
            }
            ExecutionState state = runSfn(Machines.RunGame, name, commandStart);
            return new ProcessUserCommandResponse(
                    "Starting " + makeGameDescriptor(game) + "...",
                    state.getInitialMessageUuid()
            );
        } else {
            throw makeUnknownGameException(name);
        }
    }

    @Override
    public ProcessUserCommandResponse onCommandStop(CommandStop commandStop) {
        String name = commandStop.getGameName();
        DescribeGameResponse describe = gameMetadataServiceClient.describeGame(new DescribeGameRequest(name));
        if (describe.isPresent()) {
            GameMetadata game = describe.getGame();
            if (game.getGameReadyState() != GameReadyState.RUNNING) {
                throw new RequestHandlingException("Game can't be stopped unless it's currently running");
            }
            sfnRunner.completeTask(game.getTaskCompletionToken());
            // Should get the followup message external ID (from Sfn) here somehow to send initial message.
            // TODO: Need to generally rethink concurrency/safety around server stop commands.
            return new ProcessUserCommandResponse("Stopping " + makeGameDescriptor(game) + "...");
        } else {
            throw makeUnknownGameException(name);
        }
    }

    @Override
    public ProcessUserCommandResponse onCommandAddIp(CommandAddIp commandAddIp) {
        String userId = commandAddIp.getOriginalRequest().getSenderId();
        String authUrl = networkSecurityServiceClient.generateIpAuthUrl(
                new GenerateIpAuthUrlRequest(userId)
        ).getIpAuthUrl();

        // Send a message to the user privately before returning the standard channel message.

        String welcomeMessage = "Thanks for using serverbot2. To whitelist your IP to join servers, click the following link:\n\n";
        String urlParagraph = authUrl + "\n\n";
        String reassurance = "This will detect your IP and add it to the firewall. If you've done this before, it replaces your last IP.\n\n";
        String why = "(You're seeing this message because you sent an 'addip' message (ID "
                + commandAddIp.getOriginalRequest().getMessageId() + ") in the serverbot2 '"
                + commandAddIp.getOriginalRequest().getChannel().toLowerCase() + "' channel. Exposing these servers publicly "
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

    private String makeGameDescriptor(GameMetadata metadata) {
        return metadata.getGameName() + " (" + metadata.getFullName() + ")";
    }

    private ExecutionState runSfn(Machines machine, String gameName, AbstractCommandDto context) {
        return sfnRunner.startExecution(
                machine,
                gameName,
                context.getOriginalRequest().getMessageId(),
                context.getOriginalRequest().getSenderId());
    }

    private RequestValidationException makeUnknownGameException(String gameName) {
        return new RequestValidationException("'" + gameName + "' isn't a registered game.");
    }

}
