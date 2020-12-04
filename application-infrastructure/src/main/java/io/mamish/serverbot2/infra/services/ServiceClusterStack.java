package io.mamish.serverbot2.infra.services;

import io.mamish.serverbot2.infra.util.ManagedPolicies;
import io.mamish.serverbot2.sharedutil.Utils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.CfnAutoScalingGroup.*;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.IamInstanceProfileProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.InstanceMarketOptionsProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.LaunchTemplateDataProperty;
import software.amazon.awscdk.services.ec2.CfnLaunchTemplate.MonitoringProperty;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.CfnCluster.CapacityProviderStrategyItemProperty;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ServiceClusterStack extends Stack {

    private static final String CLUSTER_NAME = "ServiceCluster";
    private static final String CONTAINER_INSTANCE_USERDATA = "#!/bin/bash\n" +
            "echo ECS_CLUSTER="+CLUSTER_NAME+" >> /etc/ecs/ecs.config";

    private final CfnInstanceProfile containerInstanceProfile;
    private final SecurityGroup containerInstanceSecurityGroup;
    private final Map<AmiHardwareType,CfnLaunchTemplate> launchTemplatesByHardwareType = new EnumMap<>(AmiHardwareType.class);
    private final CfnCluster serviceCluster;

    public ServiceClusterStack(Construct parent, String id, CommonStack commonStack) {
        super(parent, id);

        Role containerInstanceRole = Role.Builder.create(this, "ContainerInstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicies.ECS_DEFAULT_INSTANCE_POLICY))
                .build();
        containerInstanceProfile = CfnInstanceProfile.Builder.create(this, "ContainerInstanceProfile")
                .roles(List.of(containerInstanceRole.getRoleName()))
                .build();

        containerInstanceSecurityGroup = SecurityGroup.Builder.create(this, "ContainerInstanceSecurityGroup")
                .allowAllOutbound(true)
                .build();

        launchTemplatesByHardwareType.put(AmiHardwareType.STANDARD, makeContainerInstanceTemplate(AmiHardwareType.STANDARD));
        launchTemplatesByHardwareType.put(AmiHardwareType.ARM, makeContainerInstanceTemplate(AmiHardwareType.ARM));

        LaunchTemplateProperty mixedInstancesTemplate = LaunchTemplateProperty.builder()
                .launchTemplateSpecification(makeLatestTemplateSpecification(launchTemplatesByHardwareType.get(AmiHardwareType.STANDARD)))
                .overrides(List.of(
                        makeInstanceTypeOverride("m4.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("m5.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("m5a.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("r4.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("r5.large", AmiHardwareType.STANDARD),
                        makeInstanceTypeOverride("r5a.large", AmiHardwareType.STANDARD)
                ))
                .build();
        InstancesDistributionProperty capacityOptimisedInstanceDistribution = InstancesDistributionProperty.builder()
                .spotAllocationStrategy("capacity-optimized")
                .spotInstancePools(2)
                .build();
        MixedInstancesPolicyProperty autoScalingMixedInstancesPolicy = MixedInstancesPolicyProperty.builder()
                .launchTemplate(mixedInstancesTemplate)
                .instancesDistribution(capacityOptimisedInstanceDistribution)
                .build();

        List<String> serviceSubnetIds = Utils.mapList(commonStack.getServiceVpc().getPublicSubnets(), ISubnet::getSubnetId);
        CfnAutoScalingGroup capacityAutoScalingGroup = CfnAutoScalingGroup.Builder.create(this, "CapacityAutoScalingGroup")
                .capacityRebalance(true)
                .minSize("0")
                .maxSize("5")
                .desiredCapacity("0")
                .vpcZoneIdentifier(serviceSubnetIds)
                .mixedInstancesPolicy(autoScalingMixedInstancesPolicy)
                .build();

        CfnCapacityProvider capacityProvider = CfnCapacityProvider.Builder.create(this, "CapacityProvider")
                .autoScalingGroupProvider(CfnCapacityProvider.AutoScalingGroupProviderProperty.builder()
                        // this can actually take a group name (which is what Ref returns) instead of ARN
                        .autoScalingGroupArn(capacityAutoScalingGroup.getRef()).build())
                .build();

        serviceCluster = CfnCluster.Builder.create(this, "ServiceCluster")
                .clusterName(CLUSTER_NAME)
                .capacityProviders(List.of(capacityProvider.getRef()))
                .defaultCapacityProviderStrategy(List.of(CapacityProviderStrategyItemProperty.builder()
                        .capacityProvider(capacityProvider.getRef())
                        .build()))
                .build();

    }

    private CfnLaunchTemplate makeContainerInstanceTemplate(AmiHardwareType hardwareType) {

        String ecsOptimisedImageId = EcsOptimizedImage.amazonLinux2(hardwareType).getImage(this).getImageId();

        LaunchTemplateDataProperty launchTemplateData = LaunchTemplateDataProperty.builder()
                .imageId(ecsOptimisedImageId)
                .userData(CONTAINER_INSTANCE_USERDATA)
                .iamInstanceProfile(IamInstanceProfileProperty.builder().arn(containerInstanceProfile.getAttrArn()).build())
                .monitoring(MonitoringProperty.builder().enabled(true).build())
                .instanceMarketOptions(InstanceMarketOptionsProperty.builder().marketType("spot").build())
                .securityGroupIds(List.of(containerInstanceSecurityGroup.getSecurityGroupId()))
                .build();

        return CfnLaunchTemplate.Builder.create(this, "ClusterCapacityGroupTemplate")
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
