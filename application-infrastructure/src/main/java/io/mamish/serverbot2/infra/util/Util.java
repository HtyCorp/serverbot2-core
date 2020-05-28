package io.mamish.serverbot2.infra.util;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.Parameter;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Arn;
import software.amazon.awscdk.core.ArnComponents;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    public static final IManagedPolicy POLICY_BASIC_LAMBDA_EXECUTION = ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole");
    public static final IManagedPolicy POLICY_STEP_FUNCTIONS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess");
    public static final IManagedPolicy POLICY_SQS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess");
    public static final IManagedPolicy POLICY_EC2_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2FullAccess");
    public static final IManagedPolicy POLICY_S3_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess");
    public static final IManagedPolicy POLICY_S3_READ_ONLY_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess");
    public static final IManagedPolicy POLICY_DYNAMODB_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess");

    public static Role.Builder standardLambdaRole(Construct parent, String id, List<IManagedPolicy> managedPolicies) {

        List<IManagedPolicy> combinedPolicies = new ArrayList<>();
        combinedPolicies.add(POLICY_BASIC_LAMBDA_EXECUTION);
        combinedPolicies.addAll(managedPolicies);

        return Role.Builder.create(parent, id)
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(combinedPolicies);
    }

    public static void addConfigPathReadPermissionToRole(Stack stack, IRole role, String... paths) {
        List<String> secretAndParameterArns = Arrays.stream(paths)
                .flatMap(path -> Stream.of(
                        arn(stack, null, null, "ssm", "parameter/"+path+"/*"),
                        arn(stack, null, null, "secretsmanager", "secret:"+path+"/*")
                )).collect(Collectors.toList());

        role.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of(
                        "ssm:GetParameter",
                        "secretsmanager:GetSecretValue")
                ).resources(secretAndParameterArns)
                .build());
    }

    public static void addLambdaInvokePermissionToRole(Stack stack, IRole role, String... functionNames) {
        List<String> lambdaArns = Arrays.stream(functionNames)
                .map(name -> arn(stack, null, null, "lambda", "function:"+name))
                .collect(Collectors.toList());

        role.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("lambda:Invoke"))
                .resources(lambdaArns)
                .build());
    }

    public static Function.Builder standardJavaFunction(Construct parent, String id, String moduleName, String handler) {
        return Function.Builder.create(parent, id)
                .runtime(Runtime.JAVA_11)
                .code(mavenJarAsset(moduleName))
                .handler(handler)
                .memorySize(CommonConfig.STANDARD_LAMBDA_MEMORY);
    }

    public static Function.Builder standardJavaFunction(Construct parent, String id, String moduleName, String handler, IRole role) {
        return standardJavaFunction(parent, id, moduleName, handler).role(role);
    }

    public static Code mavenJarAsset(String module) {
        String rootPath = System.getenv("CODEBUILD_SRC_DIR");
        String jarPath = IDUtils.slash( rootPath, module, "target", (module+".jar"));
        return Code.fromAsset(jarPath);
    }

    public static StringParameter.Builder instantiateConfigSsmParameter(Construct parent, String id, Parameter parameter, String value) {
        return StringParameter.Builder.create(parent, id)
                .parameterName(parameter.getName())
                .stringValue(value);
    }

    public static String arn(Stack stack, String account, String region, String service, String resource) {
        return Arn.format(ArnComponents.builder()
                .account(account)
                .region(region)
                .service(service)
                .resource(resource)
                .build(), stack);
    }

}
