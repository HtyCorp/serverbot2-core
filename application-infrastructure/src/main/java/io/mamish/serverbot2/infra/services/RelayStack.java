package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.Policies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.List;

public class RelayStack extends Stack {

    public RelayStack(Construct parent, String id, CommonStack commonStack) {
        super(parent, id);

        // Data stores for relay

        Attribute messageTablePartitionKey = Attribute.builder()
                .name(DiscordConfig.MESSAGE_TABLE_PKEY)
                .type(AttributeType.STRING)
                .build();
        // Table configured for destroy since
        Table messageTable = Table.Builder.create(this, "DiscordRelayMessageTable")
                .tableName(DiscordConfig.MESSAGE_TABLE_NAME)
                .removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .partitionKey(messageTablePartitionKey)
                .build();

        // SQS queue for requests

        Queue queue = Queue.Builder.create(this, "RequestsQueue")
                .queueName(DiscordConfig.SQS_QUEUE_NAME)
                .build();

        // ECS resources required for our Fargate service hosting the Discord relay

        Cluster cluster = Cluster.Builder.create(this, "DiscordRelayCluster")
                .vpc(commonStack.getServiceVpc())
                .build();

        Role taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(List.of(
                        Policies.SQS_FULL_ACCESS,
                        Policies.EC2_FULL_ACCESS,
                        Policies.DYNAMODB_FULL_ACCESS,
                        Policies.XRAY_FULL_ACCESS
                ))
                .build();

        Util.addConfigPathReadPermissionToRole(this, taskRole, DiscordConfig.PATH_ALL);
        Util.addLambdaInvokePermissionToRole(this, taskRole, CommandLambdaConfig.FUNCTION_NAME);

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "DiscordRelayTask")
                .compatibility(Compatibility.FARGATE)
                .cpu("256")
                .memoryMiB("512") // Note this has specific allowed values in Fargate: not arbitrary
                .taskRole(taskRole)
                .build();

        // Main container in service: the Discord relay

        LogGroup relayLogGroup = LogGroup.Builder.create(this, "DiscordRelayLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver relayLogsDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(relayLogGroup)
                .streamPrefix("DiscordRelay")
                .build());

        String dockerDirPath = Util.codeBuildPath("gen", "relay-docker");
        ContainerDefinition relayContainer = taskDefinition.addContainer("DiscordRelayContainer", ContainerDefinitionOptions.builder()
                .essential(true)
                .image(ContainerImage.fromAsset(dockerDirPath))
                .memoryReservationMiB(256)
                .logging(relayLogsDriver)
                .build());

        // Sidecar container: Xray daemon

        LogGroup xrayLogGroup = LogGroup.Builder.create(this, "XrayDaemonLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver xrayLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(xrayLogGroup)
                .streamPrefix("DiscordRelay")
                .build());

        ContainerDefinition xrayContainer = taskDefinition.addContainer("XrayDaemonContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("amazon/aws-xray-daemon"))
                .memoryReservationMiB(256)
                .logging(xrayLogDriver)
                .build());

        xrayContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.UDP)
                .containerPort(2000)
                .hostPort(2000)
                .build());

        // Finally, create the service from our complete task definition with containers

        FargateService service = FargateService.Builder.create(this, "DiscordRelayService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(true)
                .build();

    }

}