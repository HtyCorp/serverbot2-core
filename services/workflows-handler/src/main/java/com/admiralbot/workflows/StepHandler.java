package com.admiralbot.workflows;

import com.admiralbot.appdaemon.model.IAppDaemon;
import com.admiralbot.appdaemon.model.StartAppRequest;
import com.admiralbot.discordrelay.model.service.*;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.gamemetadata.model.*;
import com.admiralbot.networksecurity.model.CreateSecurityGroupRequest;
import com.admiralbot.networksecurity.model.DeleteSecurityGroupRequest;
import com.admiralbot.networksecurity.model.INetworkSecurity;
import com.admiralbot.networksecurity.model.ManagedSecurityGroup;
import com.admiralbot.sharedconfig.AppInstanceConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.NetSecConfig;
import com.admiralbot.sharedutil.IDUtils;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.Poller;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.workflows.model.ExecutionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class StepHandler {

    private final Logger logger = LoggerFactory.getLogger(StepHandler.class);

    private final Ec2Client ec2Client = SdkUtils.client(Ec2Client.builder());
    private final SqsClient sqsClient = SdkUtils.client(SqsClient.builder());
    private final AppDnsRecordManager dnsRecordManager = new AppDnsRecordManager();
    private final UbuntuAmiLocator amiLocator = new UbuntuAmiLocator();
    private final IGameMetadataService gameMetadataService = ApiClient.http(IGameMetadataService.class);
    private final INetworkSecurity networkSecurityService = ApiClient.http(INetworkSecurity.class);
    private final IDiscordService discordService = ApiClient.http(IDiscordService.class);
    private final Poller<String,Instance> instanceIdPoller = new Poller<>(
            instanceId -> ec2Client.describeInstances(r -> r.instanceIds(instanceId))
                    .reservations().get(0).instances().get(0),
            // State Machine executions have been frequently timing out when this was set to 3000x20ms time limit
            // Updated to a much larger value (10s * 21 = 210s = 3m30s)
            10*1000, 21
    );

    void createGameMetadata(ExecutionState executionState) {
        String name = executionState.getGameName();

        logger.debug("GMS client: {}", gameMetadataService);
        DescribeGameResponse game = gameMetadataService.describeGame(new DescribeGameRequest(name));
        if (game.isPresent()) {
            appendMessage(executionState.getInitialMessageUuid(), "Error: game '" + name + "' already exists.");
            throw new RequestValidationException("Game already exists in metadata service");
        }

        gameMetadataService.createGame(new CreateGameRequest(executionState.getGameName(),
                "New unnamed server"));
    }

    void lockGame(ExecutionState executionState) {
        try {
            gameMetadataService.lockGame(new LockGameRequest(executionState.getGameName()));
        } catch (RequestHandlingException e) {
            appendMessage(executionState.getInitialMessageUuid(), "Error: game is currently in use");
            throw e;
        }
    }

    void createGameResources(ExecutionState executionState) {

        String gameName = executionState.getGameName();
        String instanceFriendlyName = Joiner.kebab(AppInstanceConfig.INSTANCE_NAME_PREFIX, gameName);

        Map<String,String> tagMap = Map.of(
                "Name", instanceFriendlyName,
                "Project", "Serverbot2",
                "Purpose", "AppInstance",
                AppInstanceConfig.APP_NAME_INSTANCE_TAG_KEY, gameName
        );

        ManagedSecurityGroup newSecurityGroup = networkSecurityService.createSecurityGroup(
                new CreateSecurityGroupRequest(gameName)
        ).getCreatedGroup();

        Filter vpcIdFilter = Filter.builder()
                .name("vpc-id")
                .values(CommonConfig.APPLICATION_VPC_ID.getValue())
                .build();

        Filter securityGroupNameFilter = Filter.builder()
                .name("group-name")
                .values(NetSecConfig.APP_INSTANCE_COMMON_SG_NAME)
                .build();

        // Subnet choice: nothing fancy, just pick the first one returned by DescribeSubnets in the app VPC
        String subnetId = ec2Client.describeSubnets(r -> r.filters(vpcIdFilter))
                .subnets().get(0).subnetId();

        String commonGroupId = ec2Client.describeSecurityGroups(r -> r.filters(vpcIdFilter, securityGroupNameFilter))
                .securityGroups().get(0).groupId();

        String newInstanceId;
        try {
            InputStream templateStream = getClass().getClassLoader().getResourceAsStream("instance_init.sh.template");
            String userdataTemplate = new String(Objects.requireNonNull(templateStream).readAllBytes(), StandardCharsets.UTF_8);
            String finalUserdata = userdataTemplate.replace("${SB2::OsUserName}", AppInstanceConfig.MANAGED_OS_USER_NAME);
            String encodedUserdata = Base64.getEncoder().encodeToString(finalUserdata.getBytes(StandardCharsets.UTF_8));

            BlockDeviceMapping defaultRootDevice = BlockDeviceMapping.builder()
                    .deviceName(CommonConfig.EBS_ROOT_DEVICE_NAME_DEFAULT)
                    .ebs(ebs -> ebs.deleteOnTermination(true)
                            .volumeType(VolumeType.GP3)
                            .volumeSize(CommonConfig.EBS_ROOT_DEVICE_DEFAULT_SIZE_GB)
                    ).build();

            RunInstancesResponse runInstancesResponse = ec2Client.runInstances(r ->
                    r.imageId(amiLocator.getIdealAmi().getAmiId())
                            .subnetId(subnetId)
                            .securityGroupIds(commonGroupId, newSecurityGroup.getGroupId())
                            .instanceType(InstanceType.M5_LARGE)
                            .blockDeviceMappings(defaultRootDevice)
                            .tagSpecifications(instanceAndVolumeTags(tagMap))
                            .iamInstanceProfile(spec -> spec.name(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME))
                            .minCount(1)
                            .maxCount(1)
                            .userData(encodedUserdata));
            newInstanceId = runInstancesResponse.instances().get(0).instanceId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read instance userdata resource file", e);
        }

        String queueName = Joiner.kebab(AppInstanceConfig.QUEUE_NAME_PREFIX, gameName, IDUtils.randomIdShort());
        sqsClient.createQueue(r -> r.queueName(queueName));

        gameMetadataService.updateGame(new UpdateGameRequest(gameName, null, null,
                newInstanceId, queueName, null));

    }

    void startInstance(ExecutionState executionState) {
        String instanceId = getGameMetadata(executionState.getGameName()).getInstanceId();
        ec2Client.startInstances(r -> r.instanceIds(instanceId));
    }

    void waitInstanceReady(ExecutionState executionState) {
        String name = executionState.getGameName();

        setGameStateOrTaskToken(name, null, executionState.getTaskToken());
        appendMessage(executionState.getInitialMessageUuid(), "Waiting for host startup...");

        GameMetadata gameMetadata = getGameMetadata(name);
        String publicIp = instanceIdPoller.pollUntil(gameMetadata.getInstanceId(),
                instance -> instance.publicIpAddress() != null,
                Instance::publicIpAddress);
        dnsRecordManager.updateAppRecord(name, publicIp);
    }

    void instanceReadyNotify(ExecutionState executionState) {
        String dnsLocation = dnsRecordManager.getLocationString(executionState.getGameName());

        appendMessage(executionState.getInitialMessageUuid(),
                "Server host is up at " + dnsLocation + ".\n" +
                        "Use /files or /terminal to connect to it and edit your server, then /stop to finish.");
    }

    void instanceReadyStartServer(ExecutionState executionState) {
        GameMetadata gameMetadata = getGameMetadata(executionState.getGameName());
        String appDaemonQueueName = gameMetadata.getInstanceQueueName();
        IAppDaemon appDaemonClient = ApiClient.sqs(IAppDaemon.class, appDaemonQueueName);

        String dnsName = dnsRecordManager.getFqdn(gameMetadata.getGameName());
        String dnsNameAndIp = dnsRecordManager.getLocationString(gameMetadata.getGameName());

        String ipAuthCheckUrl = "https://"
                + NetSecConfig.AUTHORIZER_SUBDOMAIN
                + "."
                + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                + NetSecConfig.AUTHORIZER_PATH_CHECK;

        try {
            appDaemonClient.startApp(new StartAppRequest());
            logger.info("Successful StartApp call to app daemon");
            appendMessage(executionState.getInitialMessageUuid(),
                    "Started at " + dnsNameAndIp +".\n"
                    + "Connect via Steam (if supported): <steam://connect/"+dnsName+">\n"
                    + "\n"
                    + "If you're unable to connect:\n"
                    + " * Ensure your IP is whitelisted (check at "+ipAuthCheckUrl+" or type /addip to whitelist).\n"
                    + " * For games with long load times, wait a few minutes and try again.");
        } catch (ApiServerException e) {
            logger.error("StartApp call to app daemon failed", e);
        }
    }

    void waitServerStop(ExecutionState executionState) {
        setGameStateOrTaskToken(executionState.getGameName(), GameReadyState.RUNNING, executionState.getTaskToken());
    }

    void stopInstance(ExecutionState executionState) {
        GameMetadata gameMetadata = getGameMetadata(executionState.getGameName());

        setGameStateOrTaskToken(executionState.getGameName(), GameReadyState.STOPPING, null);

        dnsRecordManager.deleteAppRecord(gameMetadata.getGameName());
        sqsClient.purgeQueue(r -> r.queueUrl(getQueueUrl(gameMetadata.getInstanceQueueName())));
        ec2Client.stopInstances(r -> r.instanceIds(gameMetadata.getInstanceId()));

        instanceIdPoller.pollUntil(gameMetadata.getInstanceId(),
                instance -> instance.state().name() == InstanceStateName.STOPPED);

        setGameStateOrTaskToken(executionState.getGameName(), GameReadyState.STOPPED, null);

        // TODO: Need to simplify 'laterMessageUuid' concept and how messages should flow.
        // Could perhaps make the Discord stop command return no message, and have this flow send the initial message.
        newMessage(executionState.getLaterMessageUuid(), gameMetadata.getGameName() + " has been stopped.");
    }

    void deleteGameResources(ExecutionState executionState) {
        String name = executionState.getGameName();
        GameMetadata gameMetadata = getGameMetadata(name);
        ec2Client.terminateInstances(r -> r.instanceIds(gameMetadata.getInstanceId()));
        sqsClient.deleteQueue(r -> r.queueUrl(getQueueUrl(gameMetadata.getInstanceQueueName())));

        instanceIdPoller.pollUntil(gameMetadata.getInstanceId(),
                instance -> instance.state().name() == InstanceStateName.TERMINATED);

        networkSecurityService.deleteSecurityGroup(new DeleteSecurityGroupRequest(name));
        gameMetadataService.deleteGame(new DeleteGameRequest(name));

        appendMessage(executionState.getInitialMessageUuid(), "All resources for " + name + " have been deleted.");
    }

    private static Collection<TagSpecification> instanceAndVolumeTags(Map<String,String> tagMap) {
        Collection<Tag> tags = tagMap.entrySet().stream()
                .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
        return List.of(
                TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(tags).build(),
                TagSpecification.builder().resourceType(ResourceType.VOLUME).tags(tags).build()
        );
    }

    private GameMetadata getGameMetadata(String gameName) {
        return gameMetadataService.describeGame(new DescribeGameRequest(gameName)).getGame();
    }

    private String getQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
    }

    private void setGameStateOrTaskToken(String gameName, GameReadyState state, String taskToken) {
        if (state == null && taskToken == null) {
            throw new IllegalArgumentException("Need either state or task token to make GMS update");
        }
        gameMetadataService.updateGame(new UpdateGameRequest(
                gameName,
                null,
                state,
                null,
                null,
                taskToken
        ));
    }

    private void newMessage(String messageExternalId, String newContent) {
        discordService.newMessage(new NewMessageRequest(newContent, messageExternalId, MessageChannel.MAIN, null));
    }

    private void appendMessage(String messageExternalId, String newContent) {
        discordService.editMessage(new EditMessageRequest(newContent, messageExternalId, EditMode.APPEND));
    }

}
