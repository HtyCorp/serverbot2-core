package io.mamish.serverbot2.sharedconfig;

public class UrlShortenerConfig {

    private UrlShortenerConfig() {}

    public static final String SUBDOMAIN = "go";

    public static final int MAX_TTL_SECONDS = 60*60*24*90; // 90 days

    public static final String URL_ADMIN_PATH = "admin";
    public static final String URL_ADMIN_SUBPATH_NEW = "new";
    public static final String URL_ADMIN_SUBPATH_NEW_JSONKEY_URL = "url";
    public static final String URL_ADMIN_SUBPATH_NEW_JSONKEY_TTLSECONDS = "ttlSeconds";

    public static final String DYNAMO_TABLE_NAME = "UrlShortenerFullUrlStore";
    public static final String TABLE_PARTITION_KEY = "id";
    public static final String TABLE_SORT_KEY = "schemaVersion";

}
