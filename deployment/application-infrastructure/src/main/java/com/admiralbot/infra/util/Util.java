package com.admiralbot.infra.util;

import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.Parameter;
import com.admiralbot.sharedconfig.Secret;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.secretsmanager.CfnSecret;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Util {

    public static Path codeBuildPath(String... segments) {
        String codeBuildDir = System.getenv("CODEBUILD_SRC_DIR");
        return Paths.get(codeBuildDir, segments);
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
        combinedPolicies.add(ManagedPolicies.BASIC_LAMBDA_EXECUTION);
        combinedPolicies.add(ManagedPolicies.XRAY_DAEMON_WRITE_ACCESS);
        combinedPolicies.addAll(managedPolicies);

        return Role.Builder.create(parent, id)
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(combinedPolicies);
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
                .code(Code.fromAsset(mavenJarPath(moduleName).toString()))
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

    public static Path mavenJarPath(String moduleName) {
        // Almost all the useful JAR artifacts are in the services directory so make this an easy default
        return mavenJarPath(moduleName, "services");
    }

    public static Path mavenJarPath(String moduleName, String subdirectory) {
        final String projectVersion = System.getProperty("serverbot2.version");
        String shadedJarFilename = moduleName + "-" + projectVersion + ".jar";
        return codeBuildPath(subdirectory, moduleName, "target", shadedJarFilename);
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
