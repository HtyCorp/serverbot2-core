package io.mamish.serverbot2.sharedconfig;

import java.util.List;

public class UrlShortenerConfig {

    private UrlShortenerConfig() {}

    public static final String SUBDOMAIN = "go";

    public static final int MAX_TTL_SECONDS = 60*60*24*90; // 90 days

    public static final String URL_ADMIN_PATH = "admin";
    public static final String URL_ADMIN_SUBPATH_NEW = "new";
    public static final String URL_ADMIN_SUBPATH_NEW_JSONKEY_URL = "url";
    public static final String URL_ADMIN_SUBPATH_NEW_JSONKEY_TTLSECONDS = "ttlSeconds";

    public static final String DYNAMO_TABLE_NAME = "UrlShortenerFullUrlTable";
    public static final String TABLE_PARTITION_KEY = "schemaVersion";
    public static final String TABLE_SORT_KEY = "id";

    // Primary system and app domains (from CommonConfig) are already allowed
    public static final List<String> ADDITIONAL_ALLOWED_DOMAINS = List.of(
            "signin.aws.amazon.com" // For federated console access for Session Manager
    );

    // There is no defined maximum length of a URL (per any RFCs):
    // Common practice is "about 2K" so we do twice that to be on the safe side.
    public static final int MAX_URL_LENGTH = 4096;

}
