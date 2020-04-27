package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.List;

public class CommandStack extends Stack {
    public CommandStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public CommandStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        AssetCode localCodeBuildCode = AssetCode.fromAsset("command-lambda/target/command-lambda.jar");

        List<IManagedPolicy> policyList = List.of(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        );
        Role functionRole = Role.Builder.create(this, "CommandFunctionExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(policyList)
                .build();

        Function function = Function.Builder.create(this, "CommandFunction")
                .code(localCodeBuildCode)
                .role(functionRole)
                .runtime(Runtime.JAVA_11)
                .build();
    }
}
