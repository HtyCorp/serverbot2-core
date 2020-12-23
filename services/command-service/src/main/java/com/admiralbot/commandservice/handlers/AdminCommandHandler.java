package com.admiralbot.commandservice.handlers;

import com.admiralbot.appdaemon.model.*;
import com.admiralbot.commandservice.AbstractCommandHandler;
import com.admiralbot.commandservice.SfnRunner;
import com.admiralbot.commandservice.SsmConsoleSession;
import com.admiralbot.commandservice.UrlShortenerClient;
import com.admiralbot.commandservice.commands.admin.*;
import com.admiralbot.commandservice.model.ProcessUserCommandResponse;
import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.discordrelay.model.service.NewMessageRequest;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.ServiceLimitException;
import com.admiralbot.gamemetadata.model.*;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.networksecurity.model.ModifyPortsRequest;
import com.admiralbot.networksecurity.model.PortPermission;
import com.admiralbot.networksecurity.model.PortProtocol;
import com.admiralbot.sharedconfig.CommandLambdaConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.Poller;
import com.admiralbot.sharedutil.Utils;
import com.admiralbot.workflows.model.ExecutionState;
import com.admiralbot.workflows.model.Machines;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommandHandler extends AbstractCommandHandler<IAdminCommandHandler> implements IAdminCommandHandler {

    private final Logger logger = LogManager.getLogger(AdminCommandHandler.class);

    private final Ec2Client ec2Client;
    private final IGameMetadataService gameMetadataServiceClient;
    private final INetworkSecurity networkSecurityServiceClient;
    private final IDiscordService discordServiceClient;
    private final UrlShortenerClient urlShortenerClient;
    private final Pattern portRangePattern;
    private final SfnRunner sfnRunner;
    private final Poller<String,Volume> volumeIdPoller;

    public AdminCommandHandler(Ec2Client ec2Client, IGameMetadataService gameMetadataServiceClient,
                               INetworkSecurity networkSecurityServiceClient, IDiscordService discordServiceClient,
                               UrlShortenerClient urlShortenerClient) {
        this.ec2Client = ec2Client;
        this.gameMetadataServiceClient = gameMetadataServiceClient;
        this.networkSecurityServiceClient = networkSecurityServiceClient;
        this.discordServiceClient = discordServiceClient;
        this.urlShortenerClient = urlShortenerClient;
        this.portRangePattern = Pattern.compile("(?<proto>[a-z]+):(?<portFrom>\\d{1,5})(?:-(?<portTo>\\d{1,5}))?");
        sfnRunner = new SfnRunner();
        volumeIdPoller = new Poller<>(volumeId -> ec2Client.describeVolumes(r -> r.volumeIds(volumeId))
                .volumes().get(0),
                1000, 8);
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
    public ProcessUserCommandResponse onCommandSetDescription(CommandSetDescription commandSetDescription) {
        String game = commandSetDescription.getGameName();
        String newDescription = commandSetDescription.getNewDescription();

        try {
            gameMetadataServiceClient.updateGame(new UpdateGameRequest(
                    game, newDescription,
                    null, null, null, null,
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

        DescribeGameResponse response = gameMetadataServiceClient.describeGame(new DescribeGameRequest(gameName));

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
            String fullTerminalUrl = session.getSessionUrl();
            String shortTerminalUrl = urlShortenerClient.getShortenedUrl(fullTerminalUrl,
                    CommandLambdaConfig.TERMINAL_SESSION_DURATION.getSeconds());

            String messageContent = "Use this login link to connect to a server terminal for " + gameName + ".";
            SimpleEmbed terminalUrlEmbed = new SimpleEmbed(shortTerminalUrl,
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

    @Override
    public ProcessUserCommandResponse onCommandBackupNow(CommandBackupNow commandBackupNow) {

        String name = commandBackupNow.getGameName();
        GameMetadata gameMetadata = fetchGameMetadata(name);

        String rootVolumeId = fetchRootVolume(gameMetadata).volumeId();
        String snapshotName = Joiner.kebab("AppInstance", "ManualBackup", name,
                "m"+commandBackupNow.getContext().getMessageId());
        String snapshotId = ec2Client.createSnapshot(r -> r.volumeId(rootVolumeId).description(snapshotName)).snapshotId();

        logger.info("Created new snapshot {} for game {}", snapshotId, name);

        return new ProcessUserCommandResponse("Created manual backup for " + name);

    }

    @Override
    public ProcessUserCommandResponse onCommandFiles(CommandFiles commandFiles) {

        String name = commandFiles.getGameName();
        GameMetadata gameMetadata = fetchGameMetadata(name);

        if (gameMetadata.getGameReadyState() != GameReadyState.RUNNING) {
            return new ProcessUserCommandResponse("Game must be running before you can edit its files (use '!start " + name + "').");
        }

        IAppDaemon instanceAppDaemonClient = ApiClient.sqs(IAppDaemon.class, gameMetadata.getInstanceQueueName());
        StartSftpServerResponse sftpResponse = instanceAppDaemonClient.startSftpServer(new StartSftpServerRequest());
        String sftpUri = buildSftpUri(name, sftpResponse.getSftpSession());

        String privateMessageContent = "Important: you need to have an SFTP client installed to use this link. WinSCP is recommended: "
                + "<https://winscp.net/eng/download.php>\n\n"
                + "Click this link to launch your client and view/edit files for "+name+":\n"
                + "<"+sftpUri+">";

        discordServiceClient.newMessage(new NewMessageRequest(
                privateMessageContent, null, null,
                commandFiles.getContext().getSenderId()
        ));

        return new ProcessUserCommandResponse("A connection URL has been sent to your private messages.");

    }

    private String buildSftpUri(String name, SftpSession session) {
        String userInfo = session.getUsername() + ":" + session.getPassword();
        String domainName = name + "." + CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();
        String portDef = ":" + NetSecConfig.APP_INSTANCE_SFTP_PORT;
        // Requires trailing slash to prevent WinSCP treating this as a direct download
        String sftpPath = "/opt/serverbot2/";
        String fingerprintParam = "fingerprint=" + session.getSshFingerprint();
        return "sftp://"
                + userInfo
                + "@" + domainName
                + portDef
                + sftpPath
                + ";" + fingerprintParam;
    }

    @Override
    public ProcessUserCommandResponse onCommandExtendDisk(CommandExtendDisk commandExtendDisk) {

        String name = commandExtendDisk.getGameName();
        GameMetadata gameMetadata = fetchGameMetadata(name);

        // Prevent modifications while instance is starting up: need to only edit while it's stopped (where the boot
        // process will resize FS automatically on next boot) or when it's running (so we can issue a resize through
        // app daemon).

        if (!Utils.equalsAny(gameMetadata.getGameReadyState(), GameReadyState.STOPPED, GameReadyState.RUNNING)) {
            throw new RequestValidationException(
                    "Can't modify a game's root disk while it's starting up or shutting down."
            );
        }

        // Check that the requested size is basically valid

        int requestedSize = commandExtendDisk.getSizeGB();
        if (requestedSize <= 0) {
            throw new RequestValidationException("You can't have negative disk space, what are you doing.");
        }
        final int MAX = CommonConfig.EBS_ROOT_DEVICE_MAX_SIZE_GB;
        if (requestedSize > MAX) {
            throw new RequestValidationException("Requested size can't be more than "+MAX+"GB.");
        }

        // Get the root volume and make sure we're not trying to shrink it

        Volume rootVolume = fetchRootVolume(gameMetadata);
        if (rootVolume.size() >= requestedSize) {
            throw new RequestValidationException(
                    "This game's root disk is already this size or larger ("+rootVolume.size()+"GB)."
            );
        }

        // Expand the root volume via EC2 API and wait until API shows the new size is in effect

        try {
            ec2Client.modifyVolume(r -> r.volumeId(rootVolume.volumeId()).size(requestedSize));
        } catch (Ec2Exception e) {
            logger.error("Got error code '{}' for ModifyVolume", e.awsErrorDetails().errorCode());
            switch (e.awsErrorDetails().errorCode()) {
                case "IncorrectModificationState":
                    logger.error("Disk modification/optimization already in progress", e);
                    throw new RequestHandlingException(
                            "This game's root disk is already being modified/optimized. Try again in a few hours."
                    );
                case "VolumeModificationRateExceeded":
                    logger.error("Request rate exceeded for ModifyVolume on this volume", e);
                    throw new RequestHandlingException(
                            "This game's root disk has already been modified recently. Try again in a few hours."
                    );
                default:
                    logger.error("Unknown ModifyVolume API error", e);
                    throw new RequestHandlingException(
                            "Couldn't modify this game's root disk due to an unexpected internal error."
                    );
            }
        }

        // TODO: Should poll the instance instead of EC2 API: API eventual consistency means we might be waiting longer
        // than actually necessary to issue the resize on the instance.

        volumeIdPoller.pollUntil(rootVolume.volumeId(), volume -> volume.size() == requestedSize);

        // If the game is running, issue a filesystem resize command.

        if (gameMetadata.getGameReadyState().equals(GameReadyState.RUNNING)) {
            IAppDaemon instanceAppDaemon = ApiClient.sqs(IAppDaemon.class, gameMetadata.getInstanceQueueName());
            ExtendDiskResponse extendDiskResponse = instanceAppDaemon.extendDisk(new ExtendDiskRequest());
            String successfulExtendMessage = String.format(
                    "Extended server root disk partition (/dev/%s) to %s. The new space is usable immediately.",
                    extendDiskResponse.getRootPartitionName(), extendDiskResponse.getModifiedSize()
            );
            return new ProcessUserCommandResponse(successfulExtendMessage);
        } else {
            String pendingExtendMessage = String.format(
                    "Finished expanding server root volume to %dGB. The new space will be usable on next launch.",
                    requestedSize
            );
            return new ProcessUserCommandResponse(pendingExtendMessage);
        }


    }

    private GameMetadata fetchGameMetadata(String name) {
        DescribeGameResponse describeResponse = gameMetadataServiceClient.describeGame(new DescribeGameRequest(name));

        if (!describeResponse.isPresent()) {
            logger.error("No game found from GMS:DescribeGame for game name '{}'", name);
            throw new RequestValidationException("Error: no such game "+name+" exists.");
        }

        return describeResponse.getGame();
    }

    private Volume fetchRootVolume(GameMetadata gameMetadata) {
        String instanceId = gameMetadata.getInstanceId();

        logger.info("Searching for root volumes of instance {}", instanceId);

        DescribeVolumesResponse volumesResponse = ec2Client.describeVolumes(r -> r.filters(
                Filter.builder().name("attachment.instance-id").values(instanceId).build(),
                Filter.builder().name("attachment.device").values(CommonConfig.EBS_ROOT_DEVICE_NAMES).build()
        ));
        List<Volume> volumes = volumesResponse.volumes();

        if (volumes.isEmpty()) {
            logger.error("No volumes returned by filtered DescribeVolumes search");
            throw new RequestHandlingException("Couldn't locate the root disk for this game");
        }
        if (volumes.size() > 1) {
            logger.error("Got multiple possible root volumes. Dumping volumes response...");
            logger.error(volumesResponse.toString());
            throw new RequestHandlingException("Found multiple possible root disks for this game");
        }

        return volumes.get(0);
    }



    private String makeSessionName(String userId, String gameName) {
        // Ref: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_iam-limits.html#reference_iam-limits-entity-length
        final int MAX_LENGTH = 63;
        String sessionName = userId;

        // User (snowflake) IDs will never actually exceed this, but leaving it in case the name format is changed.
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
        } catch (ServiceLimitException e) {
            e.printStackTrace();
            throw new RequestHandlingException("Unable to add more ports due to account limits.");
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
