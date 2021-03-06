package com.admiralbot.sharedconfig;

import java.time.Duration;

/**
 * Configuration values used by command handling Lambda function and shared to other packages.
 */
public class CommandLambdaConfig {

    public static final String PATH = "command-lambda";

    public static final String PATH_PRIVATE = PATH + "/private";

    public static final Secret TERMINAL_FEDERATION_ACCESS_KEY = new Secret(PATH_PRIVATE,
            "terminal-federation-access-key");
    public static final Duration TERMINAL_SESSION_DURATION = Duration.ofHours(8);

}
