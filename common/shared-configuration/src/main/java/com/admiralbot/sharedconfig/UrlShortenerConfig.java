package com.admiralbot.sharedconfig;

import java.util.List;

public class UrlShortenerConfig {

    private UrlShortenerConfig() {}

    public static final String PATH_PRIVATE = "url-shortener/private";

    public static final String SUBDOMAIN = "go";

    public static final int MAX_TTL_SECONDS = 60*60*24*90; // 90 days

    public static final String DYNAMO_TABLE_NAME = "UrlShortenerFullUrlTable";
    public static final String TABLE_PARTITION_KEY = "id";
    public static final String TABLE_SORT_KEY = "schemaVersion";

    // Primary system and app domains (from CommonConfig) are already allowed
    public static final List<String> ADDITIONAL_ALLOWED_DOMAINS = List.of(
            "signin.aws.amazon.com" // For federated console access for Session Manager
    );

    // There is no defined maximum length of a URL (per any RFCs):
    // Common practice is "about 2K" so we do twice that to be on the safe side.
    public static final int MAX_URL_LENGTH = 4096;

    public static String PREFERENCES_DYNAMO_TABLE_NAME = "UrlDeliveryUserPreferences";
    public static String PREFERENCES_TABLE_PARTITION_KEY = "userId";

    public static Secret PUSH_API_KEY_PAIR = new Secret(PATH_PRIVATE, "push-api-key-pair");

}
