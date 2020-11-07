package io.mamish.serverbot2.sharedconfig;

import java.time.Duration;

public class NetSecConfig {

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
    public static final int MAX_USER_IP_ADDRESSES = 80;

    public static final String KMS_ALIAS = "NetSecGeneralKey";

    public static final String AUTHORIZER_SUBDOMAIN = "ip";
    public static final String AUTHORIZER_SUBDOMAIN_LEGACY = "ipauth";
    public static final String AUTHORIZER_PATH_AUTHORIZE = "/authorize";
    public static final String AUTHORIZER_PATH_PARAM_TOKEN = "token";
    public static final String AUTHORIZER_PATH_CHECK = "/check";

    public static final Duration AUTH_URL_MEMBER_TTL = Duration.ofDays(90);
    public static final Duration AUTH_URL_GUEST_TTL = Duration.ofDays(3);

}
