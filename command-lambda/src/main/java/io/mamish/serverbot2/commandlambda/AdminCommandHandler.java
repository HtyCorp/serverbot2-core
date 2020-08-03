package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.commands.admin.*;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.SimpleEmbed;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.networksecurity.model.ModifyPortsRequest;
import io.mamish.serverbot2.networksecurity.model.PortPermission;
import io.mamish.serverbot2.networksecurity.model.PortProtocol;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import io.mamish.serverbot2.workflow.model.Machines;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommandHandler extends AbstractCommandHandler<IAdminCommandHandler> implements IAdminCommandHandler {

    private final Logger logger = LogManager.getLogger(AdminCommandHandler.class);

    private final INetworkSecurity networkSecurityServiceClient;
    private final IGameMetadataService gameMetadataServiceClient;
    private final IDiscordService discordServiceClient;
    private final Pattern portRangePattern;

    private final SfnRunner sfnRunner = new SfnRunner();

    public AdminCommandHandler() {
        logger.trace("Initialising NetSec client");
        networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
        logger.trace("Initialising GameMetadata client");
        gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);
        logger.trace("Initialising port regex");
        discordServiceClient = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);
        logger.trace("Initialising port regex");
        portRangePattern = Pattern.compile("(?<proto>[a-z]+):(?<portFrom>\\d{1,5})(?:-(?<portTo>\\d{1,5}))?");
        logger.trace("Finished constructor");
    }

    @Override
    protected Class<IAdminCommandHandler> getHandlerType() {
        return IAdminCommandHandler.class;
    }

    @Override
    protected IAdminCommandHandler getHandlerInstance() {
        return this;
    }

    @Override
    public ProcessUserCommandResponse onCommandNewGame(CommandNewGame commandNewGame) {
        String name = commandNewGame.getGameName();
        if (CommonConfig.RESERVED_APP_NAMES.contains(commandNewGame.getGameName())) {
            throw new RequestValidationException("'" + name + "' is a reserved name and can't be used.");
        }
        ExecutionState state = sfnRunner.startExecution(
                Machines.CreateGame,
                name,
                commandNewGame.getContext().getMessageId(),
                commandNewGame.getContext().getSenderId()
        );
        return new ProcessUserCommandResponse(
                "Creating new game '" + name + "'...",
                state.getInitialMessageUuid()
        );
    }

    @Override
    public ProcessUserCommandResponse onCommandUpdateDescription(CommandUpdateDescription commandUpdateDescription) {
        String game = commandUpdateDescription.getGameName();
        String newDescription = commandUpdateDescription.getNewDescription();

        try {
            UpdateGameResponse response = gameMetadataServiceClient.updateGame(new UpdateGameRequest(
                    game,
                    newDescription,
                    null,
                    null,
                    null,
                    null,
                    true
            ));
        } catch (RequestValidationException e) {
            logger.warn("RequestValidationException from GMS: assuming game does not exist. Message: " + e.getMessage());
            throw new RequestValidationException("Can't update " + game + ": no such game exists.");
        }

        return new ProcessUserCommandResponse("Updated description for " + game);
    }

    @Override
    public ProcessUserCommandResponse onCommandDeleteGame(CommandDeleteGame commandDeleteGame) {
        String name = commandDeleteGame.getGameName();

        DescribeGameResponse response = gameMetadataServiceClient.describeGame(new DescribeGameRequest(name));
        if (!response.isPresent()) {
            throw new RequestValidationException("Can't delete " + name + ": no such game exists.");
        }
        if (response.getGame().getGameReadyState() != GameReadyState.STOPPED) {
            throw new RequestValidationException("Can't delete " + name + ": game is currently in use.");
        }

        ExecutionState state = sfnRunner.startExecution(
                Machines.DeleteGame,
                name,
                commandDeleteGame.getContext().getMessageId(),
                commandDeleteGame.getContext().getSenderId()
        );

        return new ProcessUserCommandResponse(
                "Deleting " + name +"...",
                state.getInitialMessageUuid()
        );
    }

    @Override
    public ProcessUserCommandResponse onCommandOpenPort(CommandOpenPort commandOpenPort) {
        return runPortModifyCommand(commandOpenPort.getGameName(), commandOpenPort.getPortRange(), true);
    }

    @Override
    public ProcessUserCommandResponse onCommandClosePort(CommandClosePort commandClosePort) {
        return runPortModifyCommand(commandClosePort.getGameName(), commandClosePort.getPortRange(), false);
    }

    @Override
    public ProcessUserCommandResponse onCommandTerminal(CommandTerminal commandTerminal) {
        String gameName = commandTerminal.getGameName();

        DescribeGameResponse response = gameMetadataServiceClient.describeGame(
                new DescribeGameRequest(gameName)
        );

        String baseErrorString = "Can't connect to '" + gameName + "': ";

        if (!response.isPresent()) {
            return new ProcessUserCommandResponse(baseErrorString + "not a valid game");
        }

        if (response.getGame().getGameReadyState() != GameReadyState.RUNNING) {
            return new ProcessUserCommandResponse(baseErrorString + "game isn't running yet");
        }

        SsmConsoleSession session = new SsmConsoleSession(response.getGame().getInstanceId(),
                makeSessionName(commandTerminal.getContext().getSenderId(), gameName));

        try {
            String terminalUrl = session.getSessionUrl();

            String messageContent = "Use this login link to connect to a server terminal for " + gameName + ".";
            SimpleEmbed terminalUrlEmbed = new SimpleEmbed(terminalUrl,
                    "Terminal login link",
                    "Opens a terminal session for the server running " + gameName);

            discordServiceClient.newMessage(new NewMessageRequest(
                    messageContent,
                    null,
                    null,
                    commandTerminal.getContext().getSenderId(),
                    terminalUrlEmbed
            ));
            return new ProcessUserCommandResponse(
                    "A login link has been sent to your private messages.");
        } catch (InterruptedException | IOException e) {
            logger.error("Error during terminal session URL generate", e);
            return new ProcessUserCommandResponse(
                    "Sorry, something went wrong while generating the terminal login link.");
        }

    }

    private String makeSessionName(String userId, String gameName) {
        // Ref: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_iam-limits.html#reference_iam-limits-entity-length
        final int MAX_LENGTH = 63;
        String sessionName = userId;

        // User (snowflake) IDs will never actually exceed this, but leaving in in case I change the name format.
        if (sessionName.length() > MAX_LENGTH) {
            sessionName = sessionName.substring(0, MAX_LENGTH);
        }
        return sessionName;
    }

    private ProcessUserCommandResponse runPortModifyCommand(String gameName, String portRange, boolean addNotRemove) {
        List<PortPermission> permission = List.of(parsePortRangeString(portRange));
        try {
            networkSecurityServiceClient.modifyPorts(new ModifyPortsRequest(
                    gameName,
                    addNotRemove ? permission : null,
                    addNotRemove ? null : permission
            ));
        } catch (ApiServerException e) {
            e.printStackTrace();
            throw new RequestHandlingException("Unable to change ports as requested.");
        }
        return new ProcessUserCommandResponse("Updated server ports for game " + gameName);
    }

    private PortPermission parsePortRangeString(String portRange) {
        Matcher m = portRangePattern.matcher(portRange);

        if (!m.matches()) {
            throw new RequestValidationException("The given port range isn't valid. You must include the protocol and " +
                    "either a single port or a port range, e.g. 'udp:12345' or 'tcp:7000-8000'.");
        }

        String inputProto = m.group("proto");
        String inputPortFrom = m.group("portFrom");
        String inputPortTo = m.group("portTo");

        PortProtocol proto;
        int portFrom;
        int portTo;
        try {
            proto = PortProtocol.fromLowerCaseName(inputProto);
            if (proto == PortProtocol.ICMP) {
                throw new IllegalArgumentException("ICMP is a valid internal port type but is not allowed by " +
                        "Discord-level port modify commands");
            }
        } catch (IllegalArgumentException e) {
            throw new RequestValidationException(inputProto + " isn't a valid protocol name (must be 'udp' or 'tcp')");
        }

        try {
            portFrom = Integer.parseInt(inputPortFrom);
            if (inputPortTo == null) {
                portTo = portFrom;
            } else {
                portTo = Integer.parseInt(inputPortTo);
            }
        } catch (IllegalArgumentException e) {
            throw new RequestValidationException("The given ports aren't valid numbers.");
        }

        validatePortNumber(portFrom);
        validatePortNumber(portTo);

        if (portFrom > portTo) {
            int tmp = portTo;
            portTo = portFrom;
            portFrom = tmp;
        }

        return new PortPermission(proto, portFrom, portTo);
    }

    private void validatePortNumber(int portNumber) {
        if (portNumber < 1024 || portNumber > 65535) {
            throw new RequestValidationException("Invalid port number " + portNumber + ": must be in range 1024-65535");
        }
    }

}
