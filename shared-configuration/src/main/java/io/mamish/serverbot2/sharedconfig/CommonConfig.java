package io.mamish.serverbot2.sharedconfig;

import software.amazon.awssdk.regions.Region;

public class CommonConfig {

    public static final String COMMAND_SIGIL_CHARACTER = "!";

    public static final Region REGION = Region.AP_SOUTHEAST_2;
    public static final String REGION_NAME = "ap-southeast-2";

    public static final int DEFAULT_SQS_WAIT_TIME_SECONDS = 20;

}
