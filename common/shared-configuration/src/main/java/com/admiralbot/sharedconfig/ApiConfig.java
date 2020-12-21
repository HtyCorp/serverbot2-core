package com.admiralbot.sharedconfig;

public class ApiConfig {

    public static final String JSON_REQUEST_TARGET_KEY = "xApiTarget";
    public static final String JSON_REQUEST_ID_KEY = "xRequestId";
    // For SQS request-response APIs only
    public static final String JSON_REQUEST_QUEUE_KEY = "xQueueUrl";

    public static final String JSON_RESPONSE_CONTENT_KEY = "response";
    public static final String JSON_RESPONSE_ERROR_KEY = "error";
    // API Gateway returns this one automatically, e.g. if a request fails IAM authorization
    public static final String JSON_RESPONSE_GATEWAY_MESSAGE_KEY = "message";

    public static final int CLIENT_DEFAULT_TIMEOUT = 20;

    public static final String TEMP_QUEUE_URL_PREFIX = "ApiClientTempQueue";

    // (Ideally) Base path for requests by API Gateway to services on ECS
    // Not currently usable since path mapping doesn't seem to be possible
    public static final String REQUEST_INTERNAL_BASE_PATH = "/api";

}
