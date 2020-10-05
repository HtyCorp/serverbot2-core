package io.mamish.serverbot2.sharedconfig;

import java.util.List;

public class LambdaWarmerConfig {

    private LambdaWarmerConfig() {}

    // Special payload submitted by the Lambda warmer to indicate a request that shouldn't be actioned
    // Lambda payloads must be a JSON type, so this is a quoted JSON string
    public static final String WARMER_PING_PAYLOAD_JSON_STRING = "\"special:WarmerPing\"";

    public static final int WARMER_PING_INTERVAL_SECONDS = 10 * 60;

    public static final List<String> FUNCTION_NAMES_TO_WARM = List.of(
            CommandLambdaConfig.FUNCTION_NAME,
            GameMetadataConfig.FUNCTION_NAME,
            IpAuthConfig.FUNCTION_NAME,
            NetSecConfig.FUNCTION_NAME,
            UrlShortenerConfig.FUNCTION_NAME
    );

}
