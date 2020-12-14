package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.deploy.ApplicationStage;
import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.sharedutil.Utils;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup.*;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.IamInstanceProfileProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.LaunchTemplateDataProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.MonitoringProperty;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.CfnCapacityProvider.AutoScalingGroupProviderProperty;
import software.amazon.awscdk.services.ecs.CfnCapacityProvider.ManagedScalingProperty;
import software.amazon.awscdk.services.ecs.CfnCluster.CapacityProviderStrategyItemProperty;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

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

    public ServiceClusterStack(ApplicationStage parent, String id) {
        super(parent, id);

        Role containerInstanceRole = Role.Builder.create(this, "ContainerInstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicies.ECS_DEFAULT_INSTANCE_POLICY))
                .build();
        containerInstanceProfile = CfnInstanceProfile.Builder.create(this, "ContainerInstanceProfile")
                .roles(List.of(containerInstanceRole.getRoleName()))
                .build();

        containerInstanceSecurityGroup = SecurityGroup.Builder.create(this, "ContainerInstanceSecurityGroup")
                .vpc(parent.getCommonResources().getServiceVpc())
                .allowAllOutbound(true)
                .build();

        launchTemplatesByHardwareType.put(AmiHardwareType.STANDARD, makeContainerInstanceTemplate(AmiHardwareType.STANDARD));
        launchTemplatesByHardwareType.put(AmiHardwareType.ARM, makeContainerInstanceTemplate(AmiHardwareType.ARM));

        LaunchTemplateProperty mixedInstancesTemplate = LaunchTemplateProperty.builder()
                .launchTemplateSpecification(makeLatestTemplateSpecification(launchTemplatesByHardwareType.get(AmiHardwareType.STANDARD)))
                .overrides(List.of(
                        /* Criteria for choosing spot instance types:
                         * - Must support ECS ENI trunking (currently m5, r5, c5, a1, m6g, r6g, c6g)
                         * - 2..16GB memory since we're running several Java microservices
                         * - Need memory more than CPU so avoid c-class unless spot availability is lacking without it
                         * - Don't use ARM types just yet (should be fine with Java on ECS, but needs testing)
                         */
                        makeInstanceTypeOverride("m5.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("m5a.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("r5.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("r5a.large", AmiHardwareType.STANDARD)
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

        List<String> serviceSubnetIds = Utils.mapList(parent.getCommonResources().getServiceVpc().getPrivateSubnets(),
                ISubnet::getSubnetId);
        CfnAutoScalingGroup capacityAutoScalingGroup = CfnAutoScalingGroup.Builder.create(this, "CapacityAutoScalingGroup")
                .capacityRebalance(true)
                // Observation: When capacity is 0, managed scaling seems to interpret this as "100% utilization",
                // resulting in capacity increase. This loops forever. Avoid it by setting minimum size = 1.
                .minSize("1")
                .maxSize("2")
                .desiredCapacity("1")
                .vpcZoneIdentifier(serviceSubnetIds)
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
