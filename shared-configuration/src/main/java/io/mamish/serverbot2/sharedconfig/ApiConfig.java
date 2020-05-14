package io.mamish.serverbot2.sharedconfig;

public class ApiConfig {

    public static final String JSON_REQUEST_TARGET_KEY = "xApiTarget";
    public static final String JSON_REQUEST_ID_KEY = "xRequestId";
    // For SQS request-response APIs only
    public static final String JSON_REQUEST_QUEUE_KEY = "xQueueUrl";

    public static final String JSON_RESPONSE_CONTENT_KEY = "response";
    public static final String JSON_RESPONSE_ERROR_KEY = "error";

    public static final int CLIENT_DEFAULT_TIMEOUT = 10;

    public static final String TEMP_QUEUE_URL_PREFIX = "ApiClientTempQueue";

}
