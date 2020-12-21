package io.mamish.serverbot2.infra.constructs;

import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.IGrantable;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;
import software.amazon.awscdk.services.servicediscovery.Service;
import software.amazon.awssdk.core.SdkBytes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class EcsMicroservice extends Construct implements IGrantable {

    private final Role taskRole;
    private final Service internalDiscoveryService;

    public Role getTaskRole() {
        return taskRole;
    }

    public Service getInternalDiscoveryService() {
        return internalDiscoveryService;
    }

    public EcsMicroservice(Stack parent, String id, ApplicationStage appStage, String internalName) {
        super(parent, id);

        taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicies.XRAY_DAEMON_WRITE_ACCESS
                ))
                .build();

        Util.addConfigPathReadPermission(parent, taskRole, CommonConfig.PATH);

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "ServerTaskDefinition")
                .compatibility(Compatibility.EC2)
                .networkMode(NetworkMode.AWS_VPC)
                .cpu("256") // CPU and mem chosen to allow 8 tasks on an m5.large (2048 CPU, ~7764 MB)
                .memoryMiB("960")
                .taskRole(taskRole)
                .build();

        // Prepare Docker dir for CDK

        Path jarSrc = Util.mavenJarPath(internalName);
        InputStream dockerfileSrc = generateDockerfileStream();

        Path serviceDockerDir = Util.codeBuildPath("application-infrastructure", "target", "docker", internalName);
        Path jarDst = serviceDockerDir.resolve("service-worker.jar");
        Path dockerfileDst = serviceDockerDir.resolve("Dockerfile");

        try {
            //noinspection ResultOfMethodCallIgnored
            serviceDockerDir.toFile().mkdirs();
            Files.copy(jarSrc, jarDst, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(dockerfileSrc, dockerfileDst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy files to docker directory", e);
        }

        // Main container: service instance built from above fat JAR

        LogGroup serverLogGroup = LogGroup.Builder.create(this, "ServerLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver serverLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(serverLogGroup)
                .streamPrefix(internalName)
                .build());
        ContainerDefinition serviceContainer = taskDefinition.addContainer("ServiceContainer", ContainerDefinitionOptions.builder()
                .memoryReservationMiB(768)
                .essential(true)
                .image(ContainerImage.fromAsset(serviceDockerDir.toString()))
                .logging(serverLogDriver)
                .build());
        serviceContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.TCP)
                .containerPort(CommonConfig.SERVICES_INTERNAL_HTTP_PORT)
                .hostPort(CommonConfig.SERVICES_INTERNAL_HTTP_PORT)
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
                .memoryReservationMiB(32)
                .image(ContainerImage.fromRegistry("amazon/aws-xray-daemon"))
                .logging(xrayLogDriver)
                .build());
        xrayContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.UDP)
                .containerPort(2000)
                .hostPort(2000)
                .build());

        // Cloud Map options for service discovery (used by API Gateway)

        CloudMapOptions cloudMapOptions = CloudMapOptions.builder()
                .name(internalName)
                .cloudMapNamespace(appStage.getCommonResources().getInternalServiceNamespace())
                .dnsRecordType(DnsRecordType.SRV)
                .dnsTtl(Duration.seconds(CommonConfig.SERVICES_INTERNAL_DNS_TTL_SECONDS))
                .build();

        // Finally, create the service from our complete task definition with containers

        SecurityGroup defaultTaskSg = SecurityGroup.Builder.create(this, "DefaultTaskSecurityGroup")
                .vpc(appStage.getCommonResources().getServiceVpc())
                .allowAllOutbound(true)
                .build();
        defaultTaskSg.addIngressRule(
                Peer.ipv4(appStage.getCommonResources().getServiceVpc().getVpcCidrBlock()),
                Port.tcp(CommonConfig.SERVICES_INTERNAL_HTTP_PORT));
        Ec2Service service = Ec2Service.Builder.create(this, "EcsService")
                .cluster(appStage.getServiceCluster().getCluster())
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .cloudMapOptions(cloudMapOptions)
                .securityGroups(List.of(defaultTaskSg))
                .placementStrategies(List.of(PlacementStrategy.packedByMemory()))
                .build();

        // Current iteration of apigwv2 constructs need a Service, not an IService, so just cast it.
        // This isn't ideal but the underlying lib code does return a concrete Service so it's safe for now.
        internalDiscoveryService = (Service) service.getCloudMapService();

    }

    @Override
    public IPrincipal getGrantPrincipal() {
        return taskRole;
    }

    private InputStream generateDockerfileStream() {
        // Assumes the pipeline account and region have a mirror repository ready, hence we need to sub those values in.
        InputStream templateStream = getClass().getResourceAsStream("/EcsServiceDockerfile.template");
        String template = SdkBytes.fromInputStream(templateStream).asUtf8String();
        String reified = template.replace("${accountId}", Util.defaultAccount())
                .replace("${region}", Util.defaultRegion());
        return SdkBytes.fromUtf8String(reified).asInputStream();
    }

}
