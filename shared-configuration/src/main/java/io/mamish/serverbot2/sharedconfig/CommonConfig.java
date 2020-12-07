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
    public static final String APPLICATION_VPC_FLOW_LOGS_GROUP_NAME = "serverbot2/app/flowlogs";

    public static final Parameter SYSTEM_ROOT_DOMAIN_NAME = new Parameter(PATH, "system-root-domain-name");
    public static final Parameter SYSTEM_ROOT_DOMAIN_ZONE_ID = new Parameter(PATH, "system-root-domain-zone-id");
    public static final Parameter APP_ROOT_DOMAIN_NAME = new Parameter(PATH, "app-root-domain-name");
    public static final Parameter APP_ROOT_DOMAIN_ZONE_ID = new Parameter(PATH, "app-root-domain-zone-id");
    public static final long APP_DNS_RECORD_TTL = 10;

    public static final String COMMAND_SIGIL_CHARACTER = "!";

    public static final int DEFAULT_SQS_WAIT_TIME_SECONDS = 20;

    public static final String SERVICES_SYSTEM_SUBDOMAIN = "services";

    public static final String LAMBDA_LIVE_ALIAS_NAME = "LIVE";
    public static final int LAMBDA_MEMORY_MB_FOR_PROVISIONED = 512;
    public static final int LAMBDA_MEMORY_MB_FOR_STANDARD = 2048;
    public static final int STANDARD_LAMBDA_TIMEOUT = 20;

    public static final Pattern APP_NAME_REGEX = Pattern.compile("[a-z0-9]{2,32}");
    public static final List<String> RESERVED_APP_NAMES = List.of(
            // Empty: this is currently unused since there are now separate 'app' and 'system' domain names,
            // so DNS name collisions will not occur like they did with the combined domain name space.
    );

    public static SystemProperty ENABLE_MOCK = new SystemProperty("serverbot2.mock");

}
