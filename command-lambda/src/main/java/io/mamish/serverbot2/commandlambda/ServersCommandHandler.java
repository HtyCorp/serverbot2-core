package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.AbstractCommandDto;
import io.mamish.serverbot2.commandlambda.commands.servers.CommandGames;
import io.mamish.serverbot2.commandlambda.commands.servers.CommandStart;
import io.mamish.serverbot2.commandlambda.commands.servers.CommandStop;
import io.mamish.serverbot2.commandlambda.commands.servers.IServersCommandHandler;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import io.mamish.serverbot2.workflow.model.Machines;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ServersCommandHandler extends AbstractCommandHandler<IServersCommandHandler> implements IServersCommandHandler {

    private final Logger logger = LogManager.getLogger(ServersCommandHandler.class);

    private final IGameMetadataService gameMetadataServiceClient;
    private final INetworkSecurity networkSecurityServiceClient;
    private final IDiscordService discordServiceClient;

    private final SfnRunner sfnRunner = new SfnRunner();

    public ServersCommandHandler() {
        logger.trace("Building GMS client");
        gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);
        logger.trace("Building NetSec client");
        networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
        logger.trace("Building DiscordRelay client");
        discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
        logger.trace("Finished constructor");
    }

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
            String output = "Game ID: " + game.getGameName() + " (use this in other commands)\n" +
                    "Game description: " + game.getFullName() + "\n" +
                    "Current status: " + game.getGameReadyState().toLowerCase();
            return new ProcessUserCommandResponse(output);
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
                output.append(game.getGameName()).append(" (").append(game.getFullName()).append(")");
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

    private String makeGameDescriptor(GameMetadata metadata) {
        return metadata.getGameName() + " (" + metadata.getFullName() + ")";
    }

    private ExecutionState runSfn(Machines machine, String gameName, AbstractCommandDto context) {
        return sfnRunner.startExecution(
                machine,
                gameName,
                context.getContext().getMessageId(),
                context.getContext().getSenderId());
    }

    private RequestValidationException makeUnknownGameException(String gameName) {
        return new RequestValidationException("'" + gameName + "' isn't a registered game.");
    }

}
