package io.mamish.serverbot2.workflow;

import io.mamish.serverbot2.appdaemon.model.IAppDaemon;
import io.mamish.serverbot2.appdaemon.model.StartAppRequest;
import io.mamish.serverbot2.discordrelay.model.service.*;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import io.mamish.serverbot2.networksecurity.model.CreateSecurityGroupRequest;
import io.mamish.serverbot2.networksecurity.model.DeleteSecurityGroupRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.networksecurity.model.ManagedSecurityGroup;
import io.mamish.serverbot2.sharedconfig.*;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StepHandler {

    private final Logger logger = LogManager.getLogger(StepHandler.class);

    private final Ec2Client ec2Client = Ec2Client.create();
    private final SqsClient sqsClient = SqsClient.create();
    private final AppDnsRecordManager dnsRecordManager = new AppDnsRecordManager();
    private final UbuntuAmiLocator amiLocator = new UbuntuAmiLocator();
    private final IGameMetadataService gameMetadataService = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);
    private final INetworkSecurity networkSecurityService = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);
    private final IDiscordService discordService = ApiClient.sqs(IDiscordService.class, DiscordConfig.SQS_QUEUE_NAME);

    void createGameMetadata(ExecutionState executionState) {
        String name = executionState.getGameName();

        if (gameMetadataService.describeGame(new DescribeGameRequest(name)).isPresent()) {
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
        String instanceFriendlyName = IDUtils.kebab(AppInstanceConfig.INSTANCE_NAME_PREFIX, gameName);

        Map<String,String> tagMap = Map.of(
                "Name", instanceFriendlyName,
                "Project", "Serverbot2",
                "Purpose", "AppInstance",
                AppInstanceConfig.APP_NAME_INSTANCE_TAG_KEY, gameName
        );

        ManagedSecurityGroup newSecurityGroup = networkSecurityService.createSecurityGroup(
                new CreateSecurityGroupRequest(gameName)
        ).getCreatedGroup();

        // Subnet choice: nothing fancy, just pick the first one returned by DescribeSubnets in the app VPC
        String subnetId = ec2Client.describeSubnets(r -> r.filters(Filter.builder()
                .name("vpc-id")
                .values(CommonConfig.APPLICATION_VPC_ID.getValue())
                .build())
        ).subnets().get(0).subnetId();

        String newInstanceId;
        try {
            InputStream userdataStream = getClass().getClassLoader().getResourceAsStream("NewInstanceUserdata.sh");
            Objects.requireNonNull(userdataStream);
            String userdataString = Base64.getEncoder().encodeToString(userdataStream.readAllBytes());

            RunInstancesResponse runInstancesResponse = ec2Client.runInstances(r ->
                    r.imageId(amiLocator.getIdealAmi().getAmiId())
                            .subnetId(subnetId)
                            .securityGroupIds(newSecurityGroup.getGroupId())
                            .instanceType(InstanceType.M5_LARGE)
                            .tagSpecifications(instanceAndVolumeTags(tagMap))
                            .iamInstanceProfile(spec -> spec.name(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME))
                            .minCount(1)
                            .maxCount(1)
                            .userData(userdataString));
            newInstanceId = runInstancesResponse.instances().get(0).instanceId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read instance userdata resource file", e);
        }

        String queueName = IDUtils.kebab(AppInstanceConfig.QUEUE_NAME_PREFIX, gameName, IDUtils.randomIdShort());
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
        appendMessage(executionState.getInitialMessageUuid(), "Waiting for server host startup...");

        GameMetadata gameMetadata = getGameMetadata(name);
        String publicIp = pollInstanceIdUntil(gameMetadata.getInstanceId(),
                instance -> instance.publicIpAddress() != null,
                Instance::publicIpAddress);
        dnsRecordManager.updateAppRecord(name, publicIp);
    }

    void instanceReadyNotify(ExecutionState executionState) {
        String dnsLocation = dnsRecordManager.getLocationString(executionState.getGameName());

        appendMessage(executionState.getInitialMessageUuid(),
                "Server host is up at " + dnsLocation + ".\nAutomated install is not yet available - use"
                + " !terminal to install application software through SSH session.");
    }

    void instanceReadyStartServer(ExecutionState executionState) {
        GameMetadata gameMetadata = getGameMetadata(executionState.getGameName());
        String appDaemonQueueName = gameMetadata.getInstanceQueueName();
        IAppDaemon appDaemonClient = ApiClient.sqs(IAppDaemon.class, appDaemonQueueName);
        String dnsLocation = dnsRecordManager.getLocationString(gameMetadata.getGameName());
        try {
            appDaemonClient.startApp(new StartAppRequest());
            logger.info("Successful StartApp call to app daemon");
            appendMessage(executionState.getInitialMessageUuid(),
                    "Started game server at " + dnsLocation +".\nFor games with long load times (e.g."
                    + " Minecraft), it might be a few minutes before you can connect.\n If your IP address isn't yet"
                    + " whitelisted to join, use !addip to whitelist it.");
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

        pollInstanceIdUntil(gameMetadata.getInstanceId(),
                instance -> instance.state().name() == InstanceStateName.STOPPED,
                null);

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

        pollInstanceIdUntil(gameMetadata.getInstanceId(),
                instance -> instance.state().name() == InstanceStateName.TERMINATED,
                null);

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
        discordService.newMessage(new NewMessageRequest(newContent, messageExternalId, MessageChannel.SERVERS, null));
    }

    private void appendMessage(String messageExternalId, String newContent) {
        discordService.editMessage(new EditMessageRequest(newContent, messageExternalId, EditMode.APPEND));
    }

    private <T> T pollInstanceIdUntil(String instanceId, Predicate<Instance> condition,
                                                   Function<Instance,T> mapper) {

        Instance instance;

        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error("Unexpected thread interrupt while polling EC2 instance condition", e);
                Thread.currentThread().interrupt();
            }

            logger.debug("Condition poll: describing EC2 instance");

            // Since this class is the only thing calling this and will have recently created/deleted/changed the
            // instance it's polling, reasonable to assume this won't ever fail due to instance-not-found
            instance = ec2Client.describeInstances(r -> r.instanceIds(instanceId))
                    .reservations().get(0).instances().get(0);
            if (condition.test(instance)) {
                logger.debug("Condition poll: condition satisfied, exiting loop");
                break;
            }
        }

        if (mapper == null) {
            logger.debug("Condition poll: no map function provided, returning null");
            return null;
        } else {
            logger.debug("Condition poll: getting result data and returning");
            return mapper.apply(instance);
        }

    }

}
