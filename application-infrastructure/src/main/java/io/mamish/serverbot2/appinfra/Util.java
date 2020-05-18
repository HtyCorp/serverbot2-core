package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.Parameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.ArrayList;
import java.util.List;

public class Util {

    static final IManagedPolicy POLICY_BASIC_LAMBDA_EXECUTION = ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole");
    static final IManagedPolicy POLICY_STEP_FUNCTIONS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess");
    static final IManagedPolicy POLICY_SQS_FULL_ACCESS = ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess");

    static Role.Builder standardLambdaRole(Construct parent, String id, List<IManagedPolicy> managedPolicies) {

        List<IManagedPolicy> combinedPolicies = new ArrayList<>();
        combinedPolicies.add(POLICY_BASIC_LAMBDA_EXECUTION);
        combinedPolicies.addAll(managedPolicies);

        return Role.Builder.create(parent, id)
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(combinedPolicies);
    }

    static Function.Builder standardJavaFunction(Construct parent, String id, String moduleName, String handler) {
        return Function.Builder.create(parent, id)
                .runtime(Runtime.JAVA_11)
                .code(mavenJarAsset(moduleName))
                .handler(handler)
                .memorySize(CommonConfig.STANDARD_LAMBDA_MEMORY);
    }

    static Function.Builder standardJavaFunction(Construct parent, String id, String moduleName, String handler, IRole role) {
        return standardJavaFunction(parent, id, moduleName, handler).role(role);
    }

    static Code mavenJarAsset(String module) {
        String rootPath = System.getenv("CODEBUILD_SRC_DIR");
        String jarPath = String.join("/", rootPath, module, "target", (module+".jar"));
        return Code.fromAsset(jarPath);
    }

    static StringParameter.Builder instantiateConfigSsmParameter(Construct parent, String id, Parameter parameter, String value) {
        return StringParameter.Builder.create(parent, id)
                .parameterName(parameter.getName())
                .stringValue(value);
    }

}
