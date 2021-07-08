package com.admiralbot.commandservice.handlers;

import com.admiralbot.commandservice.AbstractCommandHandler;
import com.admiralbot.commandservice.IpAuthMessageHelper;
import com.admiralbot.commandservice.SfnRunner;
import com.admiralbot.commandservice.commands.AbstractCommandDto;
import com.admiralbot.commandservice.commands.servers.*;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.MessageChannel;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.gamemetadata.model.*;
import com.admiralbot.workflows.model.ExecutionState;
import com.admiralbot.workflows.model.Machines;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServersCommandHandler extends AbstractCommandHandler<IServersCommandHandler> implements IServersCommandHandler {

    private final Logger logger = LogManager.getLogger(ServersCommandHandler.class);

    private final IGameMetadataService gameMetadataService;
    private final IpAuthMessageHelper ipAuthMessageHelper;

    private final SfnRunner sfnRunner = new SfnRunner();

    public ServersCommandHandler(IGameMetadataService gameMetadataService, IpAuthMessageHelper ipAuthMessageHelper) {
        this.gameMetadataService = gameMetadataService;
        this.ipAuthMessageHelper = ipAuthMessageHelper;
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
            return runShowAllGames(commandGames.getContext().getChannel());
        }
    }

    private ProcessUserCommandResponse runShowSpecificGame(String name) {
        DescribeGameResponse response = gameMetadataService.describeGame(new DescribeGameRequest(name));
        if (response.isPresent()) {
            GameMetadata game = response.getGame();
            String output = "Game ID: " + game.getGameName() + "\n" +
                    "Full name: " + game.getFullName() + "\n" +
                    "Status: " + game.getGameReadyState().toLowerCase();
            return new ProcessUserCommandResponse(output);
        } else {
            throw makeUnknownGameException(name);
        }
    }

    private ProcessUserCommandResponse runShowAllGames(MessageChannel contextChannel) {
        List<GameMetadata> games = new ArrayList<>(gameMetadataService.listGames(new ListGamesRequest()).getGames());
        games.sort(Comparator.comparing(GameMetadata::getGameName));

        final StringBuilder output = new StringBuilder();
        if (games.isEmpty()) {
            output.append("No games available yet. ").append((contextChannel == MessageChannel.ADMIN)
                    ? "Try creating one with !newgame."
                    : "Admin-channel users can create one with !newgame");
        } else {
            output.append("Available games (").append(games.size()).append("):\n");
            games.forEach(game -> {
                String statusName = game.getGameReadyState().toLowerCase();
                output.append(game.getGameName()).append(" (").append(game.getFullName()).append(")");
                output.append(", currently ").append(statusName).append("\n");
            });
        }

        return new ProcessUserCommandResponse(output.toString());
    }

    @Override
    public ProcessUserCommandResponse onCommandStart(CommandStart commandStart) {
        return processSfnStartRequest(Machines.RunGame, commandStart.getGameName(), commandStart);
    }

    @Override
    public ProcessUserCommandResponse onCommandEditGame(CommandEditGame commandEditGame) {
        return processSfnStartRequest(Machines.EditGame, commandEditGame.getGameName(), commandEditGame);
    }

    private ProcessUserCommandResponse processSfnStartRequest(Machines machine, String gameName, AbstractCommandDto context) {
        DescribeGameResponse describe = gameMetadataService.describeGame(new DescribeGameRequest(gameName));
        if (describe.isPresent()) {
            GameMetadata game = describe.getGame();
            if (game.getGameReadyState() != GameReadyState.STOPPED) {
                throw new RequestHandlingException(gameName + " isn't in a valid state to start (" +
                        game.getGameReadyState().toLowerCase() + ").");
            }
            ExecutionState state = sfnRunner.startExecution(machine, gameName,
                    context.getContext().getCommandSourceId(), context.getContext().getSenderId());
            return new ProcessUserCommandResponse(
                    "Starting " + makeGameDescriptor(game) + "...",
                    state.getInitialMessageUuid()
            );
        } else {
            throw makeUnknownGameException(gameName);
        }
    }

    @Override
    public ProcessUserCommandResponse onCommandStop(CommandStop commandStop) {
        String name = commandStop.getGameName();
        DescribeGameResponse describe = gameMetadataService.describeGame(new DescribeGameRequest(name));
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
    public ProcessUserCommandResponse onCommandAddGuestIp(CommandAddGuestIp commandAddGuestIp) {
        return ipAuthMessageHelper.handleGuestIpAuthRequest(commandAddGuestIp);
    }

    private String makeGameDescriptor(GameMetadata metadata) {
        return metadata.getGameName() + " (" + metadata.getFullName() + ")";
    }

    private RequestValidationException makeUnknownGameException(String gameName) {
        return new RequestValidationException("'" + gameName + "' isn't a registered game.");
    }

}
