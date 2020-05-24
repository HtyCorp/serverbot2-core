package io.mamish.serverbot2.workflow;

import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.gamemetadata.model.IGameMetadataService;
import io.mamish.serverbot2.gamemetadata.model.LockGameRequest;
import io.mamish.serverbot2.gamemetadata.model.UpdateGameRequest;
import io.mamish.serverbot2.sharedconfig.AppInstanceConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.GameMetadataConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.workflow.model.ExecutionState;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StepHandler {

    private final Ec2Client ec2Client = Ec2Client.create();
    private final SqsClient sqsClient = SqsClient.create();
    private final UbuntuAmiLocator amiLocator = new UbuntuAmiLocator();
    private final IGameMetadataService gameMetadataServiceClient = ApiClient.lambda(IGameMetadataService.class, GameMetadataConfig.FUNCTION_NAME);

    // TODO

    void createGameMetadata(ExecutionState executionState) {

    }

    void lockGame(ExecutionState executionState) {
        gameMetadataServiceClient.lockGame(new LockGameRequest(executionState.getGameName()));
    }

    void createGameResources(ExecutionState executionState) {

        String gameName = executionState.getGameName();
        String instanceFriendlyName = IDUtils.kebab(AppInstanceConfig.INSTANCE_NAME_PREFIX, gameName);

        Map<String,String> tagMap = Map.of(
                "Name", instanceFriendlyName,
                "Project", "Serverbot2",
                "Purpose", "AppInstance",
                "Serverbot2Game", gameName
        );

        // Subnet choice: nothing fancy, just pick the first one returned by DescribeSubnets in the app VPC
        String subnetId = ec2Client.describeSubnets(r -> r.filters(Filter.builder()
                .name("vpc-id")
                .values(CommonConfig.APPLICATION_VPC_ID.getValue())
                .build())
        ).subnets().get(0).subnetId();

        RunInstancesResponse runInstancesResponse = ec2Client.runInstances(r ->
                r.imageId(amiLocator.getIdealAmi().getAmiId())
                .instanceType(InstanceType.M5_LARGE)
                .tagSpecifications(instanceAndVolumeTags(tagMap))
                .iamInstanceProfile(r2 -> r2.name(AppInstanceConfig.COMMON_INSTANCE_PROFILE_NAME))
                .minCount(1)
                .maxCount(1)
                .keyName(AppInstanceConfig.COMMON_KEYPAIR_NAME));
        String newInstanceId = runInstancesResponse.instances().get(0).instanceId();

        String queueName = IDUtils.kebab(AppInstanceConfig.QUEUE_NAME_PREFIX, gameName);
        CreateQueueResponse createQueueResponse = sqsClient.createQueue(r -> r.queueName(queueName));

        gameMetadataServiceClient.updateGame(new UpdateGameRequest(gameName, null, null,
                newInstanceId, queueName, null));

    }

    void startInstance(ExecutionState executionState) {

    }

    void waitInstanceReady(ExecutionState executionState) {

    }

    void startServer(ExecutionState executionState) {

    }

    void waitServerStop(ExecutionState executionState) {

    }

    void stopInstance(ExecutionState executionState) {

    }

    void deleteGameResources(ExecutionState executionState) {

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

}
