package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.List;

public class CommandStack extends Stack {
    public CommandStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public CommandStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Role functionRole = Util.standardLambdaRole(this, "CommandFunctionExecutionRole", List.of(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                ManagedPolicy.fromAwsManagedPolicyName("AWSStepFunctionsFullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        )).build();

        Function serviceFunction = Util.standardJavaFunction(this, "CommandService", "command-lambda",
                "io.mamish.serverbot2.commandlambda.LambdaHandler", functionRole).build();

    }
}
