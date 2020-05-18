package io.mamish.serverbot2.sharedconfig;

import java.util.List;

/**
 * Common configuration values across the project.
 */
public class CommonConfig {

    public static final String APPLICATION_VPC_CIDR = "10.0.0.0/16";
    public static final Parameter APPLICATION_VPC_ID_PARAM = new Parameter("common-config/app-vpc-id");

    public static final String APEX_DOMAIN_NAME = "test.mamish.io";

    public static final String COMMAND_SIGIL_CHARACTER = "!";

    // See if we can get away with not including these. Would be nice to stay dynamic.
//    public static final Region REGION = Region.AP_SOUTHEAST_2;
//    public static final String REGION_NAME = "ap-southeast-2";

    public static final int DEFAULT_SQS_WAIT_TIME_SECONDS = 20;

    public static final int STANDARD_LAMBDA_MEMORY = 1024;

    public static final List<String> RESERVED_APP_NAMES = List.of(
            // Reserved group name for state tracking in network security service.
            // App names and SG names are mapped 1-to-1 so this must be a reserved app name.
            NetSecConfig.REFERENCE_SG_SUFFIX
    );

}
