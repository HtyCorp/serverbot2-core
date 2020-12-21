package com.admiralbot.infra.util;

import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;

public class ManagedPolicies {
    public static final IManagedPolicy BASIC_LAMBDA_EXECUTION = ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole");
    public static final IManagedPolicy STEP_FUNCTIONS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess");
    public static final IManagedPolicy SQS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess");
    public static final IManagedPolicy EC2_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2FullAccess");
    public static final IManagedPolicy LOGS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess");
    public static final IManagedPolicy S3_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess");
    public static final IManagedPolicy ROUTE_53_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonRoute53FullAccess");
    public static final IManagedPolicy S3_READ_ONLY_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess");
    public static final IManagedPolicy DYNAMODB_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess");
    public static final IManagedPolicy XRAY_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AWSXrayFullAccess");
    public static final IManagedPolicy XRAY_DAEMON_WRITE_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AWSXRayDaemonWriteAccess");
    public static final IManagedPolicy SSM_MANAGED_INSTANCE_CORE = ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore");
    public static final IManagedPolicy EC2_READ_ONLY_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ReadOnlyAccess");

    public static final IManagedPolicy ECS_DEFAULT_INSTANCE_POLICY = ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonEC2ContainerServiceforEC2Role");
}
