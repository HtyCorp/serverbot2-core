package io.mamish.serverbot2.sharedconfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Common configuration values across the project.
 */
public class CommonConfig {

    public static String PATH = "common-config";

    // IMPORTANT: If changing SSM parameter name, must update name in userdata resource file at:
    // workflow-service/src/main/resources/NewInstanceUserdata.txt
    public static final Parameter S3_DEPLOYED_ARTIFACTS_BUCKET = new Parameter(PATH, "deployed-artifacts-bucket");

    public static final String APPLICATION_VPC_CIDR = "10.0.0.0/16";
    public static final Parameter APPLICATION_VPC_ID = new Parameter(PATH, "app-vpc-id");

    public static final String APEX_DOMAIN_NAME = "test.mamish.io";

    public static final String COMMAND_SIGIL_CHARACTER = "!";

    // See if we can get away with not including these. Would be nice to stay dynamic.
//    public static final Region REGION = Region.AP_SOUTHEAST_2;
//    public static final String REGION_NAME = "ap-southeast-2";

    public static final int DEFAULT_SQS_WAIT_TIME_SECONDS = 20;

    public static final int STANDARD_LAMBDA_MEMORY = 3008;

    public static final Pattern APP_NAME_REGEX = Pattern.compile("[a-z][a-z0-9]{1,63}");
    public static final List<String> RESERVED_APP_NAMES = List.of(
            // Reserved group name for state tracking in network security service.
            // App names and SG name suffixes are mapped 1-to-1 so this must be a reserved app name.
            NetSecConfig.REFERENCE_SG_NAME,
            // Subdomain reserved for ipauth: a game with the same name would overwrite its DNS record
            NetSecConfig.AUTH_SUBDOMAIN
    );

    public static SystemProperty ENABLE_MOCK = new SystemProperty("serverbot2.mock");

}
