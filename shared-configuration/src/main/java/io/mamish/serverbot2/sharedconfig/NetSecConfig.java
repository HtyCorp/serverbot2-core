package io.mamish.serverbot2.sharedconfig;

public class NetSecConfig {

    public static final String FUNCTION_NAME = "NetworkSecurityService";

    public static final String SG_NAME_PREFIX = "NetSecSG";
    public static final String REFERENCE_SG_NAME = "ReferenceManagedGroup";

    public static final String KMS_ALIAS = "NetSecGeneralKey";

    public static final String AUTH_SUBDOMAIN = "ipauth";
    public static final String AUTH_PATH = "/authorize";
    public static final String AUTH_PARAM_TOKEN = "token";

}
