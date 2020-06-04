package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.admin.CommandClosePort;
import io.mamish.serverbot2.commandlambda.model.commands.admin.CommandNewGame;
import io.mamish.serverbot2.commandlambda.model.commands.admin.CommandOpenPort;
import io.mamish.serverbot2.commandlambda.model.commands.admin.IAdminCommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.ProcessUserCommandResponse;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.networksecurity.model.ModifyPortsRequest;
import io.mamish.serverbot2.networksecurity.model.PortPermission;
import io.mamish.serverbot2.networksecurity.model.PortProtocol;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import io.mamish.serverbot2.workflow.model.Machines;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommandHandler extends AbstractCommandHandler<IAdminCommandHandler> implements IAdminCommandHandler {

    private final Logger logger = LogManager.getLogger(AdminCommandHandler.class);

    private final INetworkSecurity networkSecurityServiceClient;
    private final Pattern portRangePattern;

    private final SfnRunner sfnRunner = new SfnRunner();

    public AdminCommandHandler() {
        logger.trace("Initialising NetSec client");
        networkSecurityServiceClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
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
    public ProcessUserCommandResponse onCommandOpenPort(CommandOpenPort commandOpenPort) {
        return runPortModifyCommand(commandOpenPort.getGameName(), commandOpenPort.getPortRange(), true);
    }

    @Override
    public ProcessUserCommandResponse onCommandClosePort(CommandClosePort commandClosePort) {
        return runPortModifyCommand(commandClosePort.getGameName(), commandClosePort.getPortRange(), false);
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
