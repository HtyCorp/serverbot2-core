package io.mamish.serverbot2.infra.core;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.DiscordConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;

public class RelayStack extends Stack {

    public RelayStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public RelayStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        // Data stores for relay

        Attribute messageTablePartitionKey = Attribute.builder()
                .name(DiscordConfig.MESSAGE_TABLE_PKEY)
                .type(AttributeType.STRING)
                .build();
        // Table configured for destroy since
        Table messageTable = Table.Builder.create(this, "DiscordRelayMessageTable")
                .tableName(DiscordConfig.MESSAGE_TABLE_NAME)
                //.removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .partitionKey(messageTablePartitionKey)
                .build();

        // Whole bunch of ECS (Fargate) resources

        SubnetConfiguration publicSubnetConfiguration = SubnetConfiguration.builder()
                .name("PublicSubnet")
                .subnetType(SubnetType.PUBLIC)
                .build();

        Vpc vpc = Vpc.Builder.create(this, "DiscordRelayVpc")
                .maxAzs(1)
                .natGateways(0)
                .subnetConfiguration(List.of(publicSubnetConfiguration))
                .build();

        Cluster cluster = Cluster.Builder.create(this, "DiscordRelayCluster")
                .vpc(vpc)
                .build();

        Role taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(List.of(
                        Util.POLICY_SQS_FULL_ACCESS,
                        Util.POLICY_EC2_FULL_ACCESS
                ))
                .build();

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "DiscordRelayTask")
                .compatibility(Compatibility.FARGATE)
                .cpu("512")
                .memoryMiB("1024")
                .taskRole(taskRole)
                .build();

        LogGroup ecsLogGroup = LogGroup.Builder.create(this, "DiscordRelayLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .build();
        AwsLogDriverProps logDriverProps = AwsLogDriverProps.builder()
                .logGroup(ecsLogGroup)
                .streamPrefix("DiscordRelay")
                .build();

        String dockerDirPath = System.getenv("CODEBUILD_SRC_DIR") + "/relay-docker";
        ContainerDefinition containerDef = taskDefinition.addContainer("DiscordRelayContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromAsset(dockerDirPath))
                .logging(LogDriver.awsLogs(logDriverProps))
                .build());

        FargateService service = FargateService.Builder.create(this, "DiscordRelayService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(true)
                .build();

    }

}
