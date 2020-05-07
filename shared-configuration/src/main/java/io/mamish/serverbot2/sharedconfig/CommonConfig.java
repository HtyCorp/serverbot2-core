package io.mamish.serverbot2.sharedconfig;

import software.amazon.awssdk.regions.Region;

/**
 * Common configuration values across the project.
 */
public class CommonConfig {

    public static final String APEX_DOMAIN_NAME = "test.mamish.io";

    public static final String JSON_API_TARGET_KEY = "xApiTarget";

    public static final String COMMAND_SIGIL_CHARACTER = "!";

    // See if we can get away with not including these. Would be nice to stay dynamic.
//    public static final Region REGION = Region.AP_SOUTHEAST_2;
//    public static final String REGION_NAME = "ap-southeast-2";



    public static final int DEFAULT_SQS_WAIT_TIME_SECONDS = 20;

}
