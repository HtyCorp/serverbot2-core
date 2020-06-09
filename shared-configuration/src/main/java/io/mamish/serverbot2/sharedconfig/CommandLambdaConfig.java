package io.mamish.serverbot2.sharedconfig;

/**
 * Configuration values used by command handling Lambda function and shared to other packages.
 */
public class CommandLambdaConfig {

    public static final String PATH = "command-lambda";

    public static final String PATH_PUBLIC = PATH + "/public";

    // Justifying fixed name: Lambda functions have no CFN parameters that require replacement on change.
    // Basically impossible to ever get this stuck situation so no need to worry about using generated name.
    public static final String FUNCTION_NAME = "CommandService";

    public static final Parameter TERMINAL_SESSION_ROLE_ARN = new Parameter(PATH_PUBLIC, "terminal-session-role-arn");
    public static final int TERMINAL_SESSION_ROLE_DURATION_HOURS = 12;

}
