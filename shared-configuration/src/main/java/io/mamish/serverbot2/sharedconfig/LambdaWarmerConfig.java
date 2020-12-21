package io.mamish.serverbot2.sharedconfig;

import java.util.List;

public class LambdaWarmerConfig {

    private LambdaWarmerConfig() {}

    // Special payload submitted by the Lambda warmer to indicate a request that shouldn't be actioned
    // Lambda payloads must be a JSON type, so this is a quoted JSON string
    public static final String WARMER_PING_LAMBDA_PAYLOAD = "\"special:WarmerPing\"";

    // Variant for APIGW since invocation is via API rather than directly on Lambda
    public static final String WARMER_PING_API_PATH = "/warmerPingRequest";

    public static final int WARMER_PING_INTERVAL_SECONDS = 10 * 60;

    public static final List<String> FUNCTION_NAMES_TO_WARM = List.of(
        // Empty: function prewarming is unnecessary since we use ECS now
    );

    public static final List<String> API_SUBDOMAINS_TO_WARM = List.of(
            UrlShortenerConfig.SUBDOMAIN,
            NetSecConfig.AUTHORIZER_SUBDOMAIN // TODO: move this and related defs to IpAuthConfig
    );

}
