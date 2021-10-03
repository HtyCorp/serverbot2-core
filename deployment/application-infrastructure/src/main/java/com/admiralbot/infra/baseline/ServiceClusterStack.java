package com.admiralbot.infra.baseline;

import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.sharedutil.Utils;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.IamInstanceProfileProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.LaunchTemplateDataProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.MonitoringProperty;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.CfnCapacityProvider.AutoScalingGroupProviderProperty;
import software.amazon.awscdk.services.ecs.CfnCapacityProvider.ManagedScalingProperty;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.CfnCluster.CapacityProviderStrategyItemProperty;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ServiceClusterStack extends Stack {

    private static final String CLUSTER_NAME = "ServiceCluster";
    private static final String CONTAINER_INSTANCE_USERDATA = Base64.getEncoder().encodeToString((
            "#!/bin/bash\n" +
            "echo ECS_CLUSTER="+CLUSTER_NAME+" >> /etc/ecs/ecs.config"
    ).getBytes(StandardCharsets.UTF_8));

    private final CfnInstanceProfile containerInstanceProfile;
    private final SecurityGroup containerInstanceSecurityGroup;
    private final Map<AmiHardwareType,CfnLaunchTemplate> launchTemplatesByHardwareType = new EnumMap<>(AmiHardwareType.class);

    private final ICluster serviceCluster;

    public ICluster getCluster() {
        return serviceCluster;
    }

    public ServiceClusterStack(ApplicationRegionalStage parent, String id) {
        super(parent, id);

        Role containerInstanceRole = Role.Builder.create(this, "ContainerInstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicies.ECS_DEFAULT_INSTANCE_POLICY, // Required for ECS operation
                        ManagedPolicies.SSM_MANAGED_INSTANCE_CORE // Extra policy to allow SSH for management
                )).build();
        containerInstanceProfile = CfnInstanceProfile.Builder.create(this, "ContainerInstanceProfile")
                .roles(List.of(containerInstanceRole.getRoleName()))
                .build();

        containerInstanceSecurityGroup = SecurityGroup.Builder.create(this, "ContainerInstanceSecurityGroup")
                .vpc(parent.getCommonResources().getServiceVpc())
                .allowAllOutbound(true)
                .build();
        // Allow ephemeral TCP ports, so ECS containers in bridge mode can receive HTTP requests
        containerInstanceSecurityGroup.addIngressRule(
                Peer.ipv4(parent.getCommonResources().getServiceVpc().getVpcCidrBlock()),
                Port.tcpRange(32768, 65535)
        );

        launchTemplatesByHardwareType.put(AmiHardwareType.STANDARD, makeContainerInstanceTemplate(AmiHardwareType.STANDARD));
        launchTemplatesByHardwareType.put(AmiHardwareType.ARM, makeContainerInstanceTemplate(AmiHardwareType.ARM));

        LaunchTemplateProperty mixedInstancesTemplate = LaunchTemplateProperty.builder()
                .launchTemplateSpecification(makeLatestTemplateSpecification(launchTemplatesByHardwareType.get(AmiHardwareType.STANDARD)))
                .overrides(List.of(
                        /* Criteria for choosing spot instance types:
                         * - ENI trunking not required (awsvpc network mode not used due to NAT cost)
                         * - CPU/memory balance in favour of memory due to Java usage (no c-class types)
                         * - 2GB memory or only slightly more since all but one service has been migrated to Lambda
                         * - Don't use ARM types yet (would probably work, but with some config effort)
                         * Note: These were much more critical when everything was ECS, not so much now.
                         */
                        makeInstanceTypeOverride("t3a.small", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("t3.small", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("t3a.medium", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("t3.medium", AmiHardwareType.STANDARD)
                ))
                .build();
        InstancesDistributionProperty capacityOptimisedInstanceDistribution = InstancesDistributionProperty.builder()
                .spotAllocationStrategy("capacity-optimized")
                .onDemandPercentageAboveBaseCapacity(0) // 0 = all spot, 100 = all on-demand, default = 100
                .build();
        MixedInstancesPolicyProperty autoScalingMixedInstancesPolicy = MixedInstancesPolicyProperty.builder()
                .launchTemplate(mixedInstancesTemplate)
                .instancesDistribution(capacityOptimisedInstanceDistribution)
                .build();

        List<String> servicePublicSubnetIds = Utils.map(
                parent.getCommonResources().getServiceVpc().getPublicSubnets(),
                ISubnet::getSubnetId
        );
        CfnAutoScalingGroup capacityAutoScalingGroup = CfnAutoScalingGroup.Builder.create(this, "CapacityAutoScalingGroup")
                .capacityRebalance(true)
                // Observation: When capacity is 0, managed scaling seems to interpret this as "100% utilization",
                // resulting in capacity increase. This loops forever. Avoid it by setting minimum size = 1.
                .minSize("1")
                .maxSize("3")
                .desiredCapacity("1")
                .vpcZoneIdentifier(servicePublicSubnetIds)
                .mixedInstancesPolicy(autoScalingMixedInstancesPolicy)
                .newInstancesProtectedFromScaleIn(true)
                .build();

        ManagedScalingProperty managedScaling = ManagedScalingProperty.builder()
                .status("ENABLED")
                .minimumScalingStepSize(1)
                .maximumScalingStepSize(1)
                .targetCapacity(100) // Target cluster resource utilization as percentage
                .build();
        AutoScalingGroupProviderProperty autoScalingProviderProperty = AutoScalingGroupProviderProperty.builder()
                // This can actually take a group name (which is what Ref returns) instead of ARN
                .autoScalingGroupArn(capacityAutoScalingGroup.getRef())
                .managedScaling(managedScaling)
                .managedTerminationProtection("ENABLED")
                .build();
        CfnCapacityProvider capacityProvider = CfnCapacityProvider.Builder.create(this, "CapacityProvider")
                .autoScalingGroupProvider(autoScalingProviderProperty)
                .build();

        CfnCluster cfnServiceCluster = CfnCluster.Builder.create(this, "CfnServiceCluster")
                .clusterName(CLUSTER_NAME)
                .capacityProviders(List.of(capacityProvider.getRef()))
                .defaultCapacityProviderStrategy(List.of(CapacityProviderStrategyItemProperty.builder()
                        .capacityProvider(capacityProvider.getRef())
                        .build()))
                .build();

        serviceCluster = Cluster.fromClusterAttributes(this, "ServiceCluster", ClusterAttributes.builder()
                .clusterName(cfnServiceCluster.getRef())
                .clusterArn(cfnServiceCluster.getAttrArn())
                .vpc(parent.getCommonResources().getServiceVpc())
                .defaultCloudMapNamespace(parent.getCommonResources().getInternalServiceNamespace())
                .hasEc2Capacity(true)
                .securityGroups(List.of(containerInstanceSecurityGroup))
                .build());

        // Add Xray daemon as an ECS daemon service for use by other containers

        TaskDefinition xrayDaemonTaskDef = TaskDefinition.Builder.create(this, "XrayDaemonTasKDef")
                .compatibility(Compatibility.EC2)
                .networkMode(NetworkMode.BRIDGE)
                .build();
        Permissions.addManagedPoliciesToRole(xrayDaemonTaskDef.getTaskRole(), ManagedPolicies.XRAY_DAEMON_WRITE_ACCESS);

        LogGroup xrayLogGroup = LogGroup.Builder.create(this, "XrayDaemonLogGroup")
                .retention(RetentionDays.ONE_YEAR)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        LogDriver xrayLogDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(xrayLogGroup)
                .streamPrefix("xray-daemon")
                .build());
        ContainerDefinition xrayContainer = xrayDaemonTaskDef.addContainer("XrayDaemonContainer", ContainerDefinitionOptions.builder()
                .memoryLimitMiB(128)
                .image(ContainerImage.fromRegistry("amazon/aws-xray-daemon"))
                .logging(xrayLogDriver)
                .build());
        xrayContainer.addPortMappings(PortMapping.builder()
                .protocol(Protocol.UDP)
                .containerPort(2000)
                .hostPort(2000) // Uses a reserved port on each container instance since this is a daemon service
                .build());

        Ec2Service xrayDaemonService = Ec2Service.Builder.create(this, "XrayDaemonService")
                .cluster(serviceCluster)
                .daemon(true)
                .taskDefinition(xrayDaemonTaskDef)
                .build();

    }

    private CfnLaunchTemplate makeContainerInstanceTemplate(AmiHardwareType hardwareType) {

        String ecsOptimisedImageId = EcsOptimizedImage.amazonLinux2(hardwareType).getImage(this).getImageId();

        LaunchTemplateDataProperty launchTemplateData = LaunchTemplateDataProperty.builder()
                .imageId(ecsOptimisedImageId)
                .userData(CONTAINER_INSTANCE_USERDATA)
                .iamInstanceProfile(IamInstanceProfileProperty.builder().arn(containerInstanceProfile.getAttrArn()).build())
                .monitoring(MonitoringProperty.builder().enabled(true).build())
                .securityGroupIds(List.of(containerInstanceSecurityGroup.getSecurityGroupId()))
                .build();

        return CfnLaunchTemplate.Builder.create(this, "ClusterCapacityGroupTemplate" + hardwareType.name())
                .launchTemplateData(launchTemplateData)
                .build();
    }

    private LaunchTemplateOverridesProperty makeInstanceTypeOverride(String instanceType, AmiHardwareType hardwareType) {
        CfnLaunchTemplate launchTemplate = launchTemplatesByHardwareType.get(hardwareType);
        return LaunchTemplateOverridesProperty.builder()
                .instanceType(instanceType)
                .launchTemplateSpecification(makeLatestTemplateSpecification(launchTemplate))
                .build();
    }

    private LaunchTemplateSpecificationProperty makeLatestTemplateSpecification(CfnLaunchTemplate launchTemplate) {
        return LaunchTemplateSpecificationProperty.builder()
                .launchTemplateId(launchTemplate.getRef())
                .version(launchTemplate.getAttrLatestVersionNumber())
                .build();
    }

}
