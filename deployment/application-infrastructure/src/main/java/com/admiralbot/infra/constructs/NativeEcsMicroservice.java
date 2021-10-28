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

public class NativeEcsMicroservice extends Construct implements IGrantable {

    private final Role taskRole;
    private final Service internalDiscoveryService;

    public NativeEcsMicroservice(Stack parent, String id, ApplicationRegionalStage appStage, String moduleName) {
        super(parent, id);

        taskRole = Role.Builder.create(this, "DiscordRelayRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();
        Permissions.addConfigPathRead(parent, taskRole, CommonConfig.PATH);

        // Important ref: https://aws.amazon.com/blogs/containers/how-amazon-ecs-manages-cpu-and-memory-resources/
        // Setting a task size restricts tasks to a hard CPU quota/limit when we want them to use any spare CPU
        // capacity available; instead, memory limits are set at container level.

        TaskDefinition taskDefinition = TaskDefinition.Builder.create(this, "ServerTaskDefinition")
                .compatibility(Compatibility.EC2)
                .networkMode(NetworkMode.BRIDGE)
                //.cpu("256")
                //.memoryMiB("960")
                .taskRole(taskRole)
                .build();

        // Prepare Docker dir for CDK
        InputStream dockerfileSrc = generateDockerfileStream(appStage.getEnv());

        Path servicePackagePath = Util.mavenModulePackagePath(moduleName);
        Path dockerfileDst = servicePackagePath.resolve("Dockerfile");

        try {
            Files.copy(dockerfileSrc, dockerfileDst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy files to docker directory", e);
        }

        // Main container: service instance built from above fat JAR

        String friendlyLogGroupName = Joiner.slash(DeployConfig.SERVICE_LOGS_PREFIX, "ecs", moduleName);
        LogGroup serverLogGroup = LogGroup.Builder.create(this, "ServerLogGroup")
                .logGroupName(friendlyLogGroupName)
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver serverLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(serverLogGroup)
                .streamPrefix(moduleName)
                .build());
        ContainerDefinition serviceContainer = taskDefinition.addContainer("ServiceContainer", ContainerDefinitionOptions.builder()
                .memoryLimitMiB(224)
                .essential(true)
                .image(ContainerImage.fromAsset(servicePackagePath.toString()))
                .logging(serverLogDriver)
                // 172.17.0.1 is the default gateway for Docker bridge network
                .environment(Map.of("AWS_XRAY_DAEMON_ADDRESS", "172.17.0.1:2000"))
                .build());
        serviceContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.TCP)
                .containerPort(CommonConfig.SERVICES_INTERNAL_HTTP_PORT)
                .hostPort(0) // '0' means assign ephemeral port (should be in 32768..65535 range)
                .build());

        // Cloud Map options for service discovery (used by API Gateway)

        CloudMapOptions cloudMapOptions = CloudMapOptions.builder()
                .name(moduleName)
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

    public Service getInternalDiscoveryService() {
        return internalDiscoveryService;
    }

    public Role getRole() {
        return taskRole;
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
