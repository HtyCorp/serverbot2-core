package io.mamish.serverbot2.sharedconfig;

/**
 * Configuration values used by command handling Lambda function and shared to other packages.
 */
public class CommandLambdaConfig {

    // Justifying fixed name: Lambda functions have no CFN parameters that require replacement on change.
    // Basically impossible to ever get this stuck situation so no need to worry about using generated name.
    public static final String FUNCTION_NAME = "Serverbot2CommandLambda";

}
