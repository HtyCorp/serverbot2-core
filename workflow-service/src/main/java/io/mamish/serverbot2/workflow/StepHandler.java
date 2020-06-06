package io.mamish.serverbot2.workflow;

import io.mamish.serverbot2.appdaemon.model.IAppDaemon;
import io.mamish.serverbot2.appdaemon.model.StartAppRequest;
import io.mamish.serverbot2.discordrelay.model.service.EditMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.EditMode;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordService;
import io.mamish.serverbot2.framework.client.ApiClient;
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
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StepHandler {

    private final Ec2Client ec2Client = Ec2Client.create();
    private final SqsClient sqsClient = SqsClient.create();
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
                "New game (use !setname after completing installation)"));
    }

    void lockGame(ExecutionState executionState) {
        try {
            gameMetadataService.lockGame(new LockGameRequest(executionState.getGameName()));
        } catch (RequestHandlingException e) {
            appendMessage(executionState.getInitialMessageUuid(), "Error: Can only start a game if it is stopped");
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
            String userdataString = Base64.getEncoder().encodeToString(userdataStream.readAllBytes());

            RunInstancesResponse runInstancesResponse = ec2Client.runInstances(r ->
                    r.imageId(amiLocator.getIdealAmi().getAmiId())
                            .subnetId(subnetId)
                            .securityGroupIds(newSecurityGroup.getGroupId())
                            .instanceType(InstanceType.M5_LARGE)
                            .tagSpecifications(instanceAndVolumeTags(tagMap))
                            .iamInstanceProfile(r2 -> r2.name(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME))
                            .minCount(1)
                            .maxCount(1)
                            .userData(userdataString));
            newInstanceId = runInstancesResponse.instances().get(0).instanceId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read instance userdata resource file", e);
        }

        String queueName = IDUtils.kebab(AppInstanceConfig.QUEUE_NAME_PREFIX, gameName);
        sqsClient.createQueue(r -> r.queueName(queueName));

        gameMetadataService.updateGame(new UpdateGameRequest(gameName, null, null,
                newInstanceId, queueName, null));

    }

    void startInstance(ExecutionState executionState) {
        String instanceId = getGameMetadata(executionState.getGameName()).getInstanceId();
        ec2Client.startInstances(r -> r.instanceIds(instanceId));
    }

    void waitInstanceReady(ExecutionState executionState) {
        setCallbackTaskToken(executionState.getGameName(), executionState.getTaskToken());
    }

    void startServer(ExecutionState executionState) {
        String appDaemonQueueName = getGameMetadata(executionState.getGameName()).getInstanceQueueName();
        IAppDaemon appDaemonClient = ApiClient.sqs(IAppDaemon.class, appDaemonQueueName);
        appDaemonClient.startApp(new StartAppRequest());
    }

    void waitServerStop(ExecutionState executionState) {
        setCallbackTaskToken(executionState.getGameName(), executionState.getTaskToken());
    }

    void stopInstance(ExecutionState executionState) {
        GameMetadata gameMetadata = getGameMetadata(executionState.getGameName());
        ec2Client.stopInstances(r -> r.instanceIds(gameMetadata.getInstanceId()));
        sqsClient.purgeQueue(r -> r.queueUrl(getQueueUrl(gameMetadata.getInstanceQueueName())));
    }

    void deleteGameResources(ExecutionState executionState) {
        String name = executionState.getGameName();
        GameMetadata gameMetadata = getGameMetadata(name);
        ec2Client.terminateInstances(r -> r.instanceIds(gameMetadata.getInstanceId()));
        sqsClient.deleteQueue(r -> r.queueUrl(getQueueUrl(gameMetadata.getInstanceQueueName())));
        gameMetadataService.deleteGame(new DeleteGameRequest(name));
        networkSecurityService.deleteSecurityGroup(new DeleteSecurityGroupRequest(name));
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

    private void setCallbackTaskToken(String gameName, String taskToken) {
        gameMetadataService.updateGame(new UpdateGameRequest(
                gameName,
                null,
                null,
                null,
                null,
                taskToken
        ));
    }

    private void appendMessage(String messageExternalId, String newContent) {
        discordService.editMessage(new EditMessageRequest(newContent, messageExternalId, EditMode.APPEND, false));
    }

}
