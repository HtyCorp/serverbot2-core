package io.mamish.serverbot2.sharedconfig;

import java.time.Duration;

public class NetSecConfig {

    public static final String PATH_PUBLIC = "network-security/public";

    public static final String FUNCTION_NAME = "NetworkSecurityService";

    public static final String SG_NAME_PREFIX = "NetSecSG";

    /* "If you reference a customer-managed prefix list in a security group rule, the maximum number of entries for the
     * prefix lists equals the same number of security group rules."
     * https://docs.aws.amazon.com/vpc/latest/userguide/amazon-vpc-limits.html#vpc-limits-security-groups
     *
     * Prod account has rules-per-group limit increased to 250, so list size 80 allows 3 rules per group referencing the
     * list plus 10 left over as spare.
     */
    public static final String USER_IP_PREFIX_LIST_NAME = "DiscordUserIpList";
    public static final Parameter USER_IP_PREFIX_LIST_SIZE = new Parameter(PATH_PUBLIC, "prefix-list-size");

    public static final String APP_INSTANCE_COMMON_SG_NAME = "AppInstanceCommon";
    // Uses custom SFTP port to avoid needing root access for privileged port
    // Randomly picked from an unpopular range in:
    // https://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers
    public static final int APP_INSTANCE_SFTP_PORT = 32176;

    public static final String KMS_ALIAS = "NetSecGeneralKey";

    public static final String AUTHORIZER_SUBDOMAIN = "ip";
    public static final String AUTHORIZER_SUBDOMAIN_LEGACY = "ipauth";
    public static final String AUTHORIZER_PATH_AUTHORIZE = "/authorize";
    public static final String AUTHORIZER_PATH_PARAM_TOKEN = "token";
    public static final String AUTHORIZER_PATH_CHECK = "/check";

    public static final Duration AUTH_URL_MEMBER_TTL = Duration.ofDays(90);
    public static final Duration AUTH_URL_GUEST_TTL = Duration.ofDays(3);

}
