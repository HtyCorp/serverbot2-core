package io.mamish.serverbot2.sharedconfig;

public class NetSecConfig {

    public static final String FUNCTION_NAME = "NetworkSecurityService";

    public static final String SG_NAME_PREFIX = "NetSecSG";

    /* "If you reference a customer-managed prefix list in a security group rule, the maximum number of entries for the
     * prefix lists equals the same number of security group rules."
     * https://docs.aws.amazon.com/vpc/latest/userguide/amazon-vpc-limits.html#vpc-limits-security-groups
     *
     * Because NetSec uses prefix lists in rules and default rules limit is 60, very limited. Using 2*30 for now, and
     * will increase these later (currently have a service limit increase pending in prod).
     */
    public static final int MAX_SECURITY_GROUP_RULES = 2;
    public static final int MAX_USER_IP_ADDRESSES = 30;
    public static final String USER_IP_PREFIX_LIST_NAME = "DiscordUserIpList";

    public static final String KMS_ALIAS = "NetSecGeneralKey";

    public static final String AUTH_SUBDOMAIN = "ipauth";
    public static final String AUTH_PATH = "/authorize";
    public static final String AUTH_PARAM_TOKEN = "token";

}
