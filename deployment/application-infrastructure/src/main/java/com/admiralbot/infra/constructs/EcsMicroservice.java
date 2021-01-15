package com.admiralbot.infra.constructs;

import com.admiralbot.infra.deploy.ApplicationEnv;
import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.DeployConfig;
import com.admiralbot.sharedutil.Joiner;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
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
import java.util.Map;

public class EcsMicroservice extends Construct implements IGrantable {

    private final Role taskRole;
    private final Service internalDiscoveryService;

    public Role getTaskRole() {
        return taskRole;
    }

    public Service getInternalDiscoveryService() {
        return internalDiscoveryService;
    }

    public EcsMicroservice(Stack parent, String id, ApplicationRegionalStage appStage, String internalName) {
        super(parent, id);

        taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();
        Permissions.addConfigPathRead(parent, taskRole, CommonConfig.PATH);

        // Important ref: https://aws.amazon.com/blogs/containers/how-amazon-ecs-manages-cpu-and-memory-resources/
        // Setting a task size restricts tasks to a hard CPU quota/limit when we want them to use any spare CPU
        // capacity available; instead, memory limits are set at container level.
        // Note capacity of an m5.large is approx. 2048 CPU, ~7764 MB.
        // Good divisor is 8 tasks each running at 960MB limit (and 256 CPU though this is optional).

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "ServerTaskDefinition")
                .compatibility(Compatibility.EC2)
                .networkMode(NetworkMode.BRIDGE)
                //.cpu("256")
                //.memoryMiB("960")
                .taskRole(taskRole)
                .build();

        // Prepare Docker dir for CDK

        Path jarSrc = Util.mavenJarPath(internalName);
        InputStream dockerfileSrc = generateDockerfileStream(appStage.getEnv());

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

        String friendlyLogGroupName = Joiner.slash(DeployConfig.SERVICE_LOGS_PREFIX, "ecs", internalName);
        LogGroup serverLogGroup = LogGroup.Builder.create(this, "ServerLogGroup")
                .logGroupName(friendlyLogGroupName)
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver serverLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(serverLogGroup)
                .streamPrefix(internalName)
                .build());
        ContainerDefinition serviceContainer = taskDefinition.addContainer("ServiceContainer", ContainerDefinitionOptions.builder()
                .memoryLimitMiB(896)
                .essential(true)
                .image(ContainerImage.fromAsset(serviceDockerDir.toString()))
                .logging(serverLogDriver)
                // See Xray daemon definition in ServiceClusterStack: this is the default host gateway IP on ECS AMI
                .environment(Map.of("AWS_XRAY_DAEMON_ADDRESS", "169.254.172.1:2000"))
                .build());
        serviceContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.TCP)
                .containerPort(CommonConfig.SERVICES_INTERNAL_HTTP_PORT)
                .hostPort(0) // '0' means assign ephemeral port (should be in 32768..65535 range)
                .build());

        // Cloud Map options for service discovery (used by API Gateway)

        CloudMapOptions cloudMapOptions = CloudMapOptions.builder()
                .name(internalName)
                .cloudMapNamespace(appStage.getCommonResources().getInternalServiceNamespace())
                .dnsRecordType(DnsRecordType.SRV)
                .dnsTtl(Duration.seconds(CommonConfig.SERVICES_INTERNAL_DNS_TTL_SECONDS))
                .build();

        // Finally, create the service from our complete task definition with containers

        Ec2Service service = Ec2Service.Builder.create(this, "EcsService")
                .cluster(appStage.getServiceCluster().getCluster())
                .taskDefinition(taskDefinition)
                .cloudMapOptions(cloudMapOptions)
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

    private InputStream generateDockerfileStream(ApplicationEnv env) {
        // Assumes the current account and region have a mirror repository ready, hence we need to sub those values in.
        InputStream templateStream = getClass().getResourceAsStream("/EcsServiceDockerfile.template");
        String template = SdkBytes.fromInputStream(templateStream).asUtf8String();
        String reified = template.replace("${accountId}", env.getAccountId())
                .replace("${region}", env.getRegion());
        return SdkBytes.fromUtf8String(reified).asInputStream();
    }

}
