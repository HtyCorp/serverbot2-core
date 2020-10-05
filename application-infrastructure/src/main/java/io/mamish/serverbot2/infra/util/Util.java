package io.mamish.serverbot2.infra.util;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.Parameter;
import io.mamish.serverbot2.sharedconfig.Secret;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.secretsmanager.CfnSecret;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    public static String codeBuildPath(String... path) {
        String codeBuildDir = System.getenv("CODEBUILD_SRC_DIR");
        return Paths.get(codeBuildDir, path).toString();
    }

    public static String environmentManifestPath() {
        String manifestDir = System.getenv("CODEBUILD_SRC_DIR_environment_manifest");
        return Paths.get(manifestDir, "manifest.json").toString();
    }

    public static String defaultAccount() {
        return System.getenv("CDK_DEFAULT_ACCOUNT");
    }

    public static String defaultRegion() {
        return System.getenv("CDK_DEFAULT_REGION");
    }

    public static Role.Builder standardLambdaRole(Construct parent, String id, List<IManagedPolicy> managedPolicies) {

        List<IManagedPolicy> combinedPolicies = new ArrayList<>();
        combinedPolicies.add(Policies.BASIC_LAMBDA_EXECUTION);
        combinedPolicies.add(Policies.XRAY_DAEMON_WRITE_ACCESS);
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
                .map(name -> IDUtils.colon("function", name, CommonConfig.LAMBDA_LIVE_ALIAS_NAME))
                .map(resource -> arn(stack, null, null, "lambda", resource))
                .collect(Collectors.toList());

        role.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("lambda:InvokeFunction"))
                .resources(lambdaArns)
                .build());
    }

    // This is tricky to make better since the ARN uses the APIGW ID rather than any friendly/predictable ID.
    public static void addFullExecuteApiPermissionToRole(Stack stack, IRole role) {
        String wildcardArnApisInAccountAndRegion = arn(stack, null, null, "execute-api", "*");
        role.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("execute-api:Invoke"))
                .resources(List.of(wildcardArnApisInAccountAndRegion))
                .build());
    }

    public static Alias highMemJavaFunction(Construct parent, String id, String moduleName, String handler,
                                            Consumer<Function.Builder> editFunction) {
        return baseJavaFunction(parent, id, moduleName, handler, CommonConfig.LAMBDA_MEMORY_MB_FOR_STANDARD,
                editFunction, null);
    }

    public static Alias provisionedJavaFunction(Construct parent, String id, String moduleName, String handler,
                                                int provisionedConcurrency, Consumer<Function.Builder> editFunction) {
        return baseJavaFunction(parent, id, moduleName, handler, CommonConfig.LAMBDA_MEMORY_MB_FOR_PROVISIONED,
                editFunction, b -> b.provisionedConcurrentExecutions(provisionedConcurrency));
    }

    private static Alias baseJavaFunction(Construct parent, String id, String moduleName, String handler, int memory,
                                             Consumer<Function.Builder> functionEditor, Consumer<Alias.Builder> aliasEditor) {
        Function.Builder functionBuilder = Function.Builder.create(parent, id)
                .runtime(Runtime.JAVA_11)
                .code(mavenJarAsset(moduleName))
                .memorySize(memory)
                .handler(handler)
                .tracing(Tracing.ACTIVE)
                .timeout(Duration.seconds(CommonConfig.STANDARD_LAMBDA_TIMEOUT));
        if (functionEditor != null) {
            functionEditor.accept(functionBuilder);
        }
        Function function = functionBuilder.build();

        Alias.Builder aliasBuilder = Alias.Builder.create(parent, id+"Alias")
                .version(function.getCurrentVersion())
                .aliasName(CommonConfig.LAMBDA_LIVE_ALIAS_NAME);
        if (aliasEditor != null) {
            aliasEditor.accept(aliasBuilder);
        }
        return aliasBuilder.build();

    }

    public static Code mavenJarAsset(String moduleName) {
        // Should make this refer to home or maybe current working directory if env not found
        String rootPath = System.getenv("CODEBUILD_SRC_DIR");
        final String projectVersion = System.getProperty("serverbot2.version");
        final String assemblyDescriptor = "jar-with-dependencies";
        String baseFileName = IDUtils.kebab(moduleName, projectVersion, assemblyDescriptor);
        String fullFileName = baseFileName + ".jar";
        String jarPath = IDUtils.slash( rootPath, moduleName, "target", fullFileName);
        return Code.fromAsset(jarPath);
    }

    public static StringParameter instantiateConfigSsmParameter(Construct parent, String id, Parameter parameter, String value) {
        return StringParameter.Builder.create(parent, id)
                .parameterName(parameter.getName())
                .stringValue(value)
                .build();
    }

    public static CfnSecret instantiateConfigSecret(Construct parent, String id, Secret configSecret, String value) {
        return CfnSecret.Builder.create(parent, id)
                .name(configSecret.getName())
                .secretString(value)
                .build();
    }

    public static void setConstructCfnRemovalPolicy(Construct construct, RemovalPolicy policy) {
        // Ref: https://docs.aws.amazon.com/cdk/latest/guide/resources.html#resources_removal
        CfnResource resource = (CfnResource) construct.getNode().findChild("Resource");
        resource.applyRemovalPolicy(policy);
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
