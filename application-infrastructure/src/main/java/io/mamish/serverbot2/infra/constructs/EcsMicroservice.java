package io.mamish.serverbot2.infra.constructs;

import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;

public class EcsMicroservice extends Construct {

    private final Role taskRole;

    public Role getTaskRole() {
        return taskRole;
    }

    public EcsMicroservice(Construct parent, String id, ApplicationStage appStage, EcsMicroserviceProps props) {
        super(parent, id);

        taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicies.XRAY_DAEMON_WRITE_ACCESS
                ))
                .build();

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "ServerTaskDefinition")
                .compatibility(Compatibility.EC2)
                .cpu("256")
                .memoryMiB("1024") // Note this has specific allowed values in Fargate: not arbitrary
                .taskRole(taskRole)
                .build();

        // Main container in service: the Discord relay

        LogGroup serverLogGroup = LogGroup.Builder.create(this, "ServerLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver serverLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(serverLogGroup)
                .streamPrefix(props.getJavaModuleName())
                .build());
        String dockerDirPath = Util.codeBuildPath(props.getJavaModuleName(), "target", "docker");
        taskDefinition.addContainer("ServiceContainer", ContainerDefinitionOptions.builder()
                .essential(true)
                .image(ContainerImage.fromAsset(dockerDirPath))
                .logging(serverLogDriver)
                .build());

        // Sidecar container: Xray daemon

        LogGroup xrayLogGroup = LogGroup.Builder.create(this, "XrayDaemonLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver xrayLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(xrayLogGroup)
                .streamPrefix("xray-daemon")
                .build());
        ContainerDefinition xrayContainer = taskDefinition.addContainer("XrayDaemonContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("amazon/aws-xray-daemon"))
                .logging(xrayLogDriver)
                .build());
        xrayContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.UDP)
                .containerPort(2000)
                .hostPort(2000)
                .build());

        // Finally, create the service from our complete task definition with containers

        FargateService service = FargateService.Builder.create(this, "DiscordRelayService")
                .cluster(appStage.getServiceCluster().getCluster())
                .taskDefinition(taskDefinition)
                .assignPublicIp(true)
                .build();

    }

}
