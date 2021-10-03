package com.admiralbot.framework.server.lambdaruntime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class LambdaRuntimeClient {

    // Name of env var containing runtime API endpoint value
    private static final String API_ENDPOINT_ENV_VAR = "AWS_LAMBDA_RUNTIME_API";

    // Lambda runtime API paths
    private static final String API_VERSION_PATH = "/2018-06-01";
    private static final String NEXT_INVOCATION_PATH = "/runtime/invocation/next";
    private static final String INIT_ERROR_PATH = "/runtime/init/error";
    private static final String INVOCATION_RESPONSE_PATH_FORMAT = "/runtime/invocation/%s/response";
    private static final String INVOCATION_ERROR_PATH_FORMAT = "/runtime/invocation/%s/error";

    // Lambda runtime API response header names
    private static final String INVOCATION_ID_HEADER = "Lambda-Runtime-Aws-Request-Id";
    private static final String DEADLINE_MS_HEADER = "Lambda-Runtime-Deadline-Ms";
    private static final String LAMBDA_ARN_HEADER = "Lambda-Runtime-Invoked-Function-Arn";
    private static final String XRAY_TRACE_ID_HEADER = "Lambda-Runtime-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(LambdaRuntimeClient.class);
    private static final Gson gson = new Gson();

    private final String apiEndpointHost;
    private final SdkHttpClient httpClientStandard;
    private final SdkHttpClient httpClientNoTimeout;

    public LambdaRuntimeClient() {
        apiEndpointHost = Objects.requireNonNull(System.getenv(API_ENDPOINT_ENV_VAR));
        // The HTTP client for invocation long-polling needs an infinite timeout
        httpClientNoTimeout = UrlConnectionHttpClient.builder().socketTimeout(Duration.ZERO).build();
        // The HTTP client for all other paths can use a much lower timeout (response should be basically instant)
        httpClientStandard = UrlConnectionHttpClient.builder().socketTimeout(Duration.ofSeconds(3)).build();
    }

    public void postInitError(String errorMessage, String errorType, List<String> stackTrace) {
        Map<String,String> headers = Map.of(LambdaError.ERROR_TYPE_HEADER, errorType);
        LambdaError lambdaError = new LambdaError(errorMessage, errorType, stackTrace);
        call(SdkHttpMethod.POST, INIT_ERROR_PATH, headers, gson.toJson(lambdaError));
    }

    public LambdaInvocation getNextInvocation() {
        HttpExecuteResponse response = call(httpClientNoTimeout, SdkHttpMethod.GET, NEXT_INVOCATION_PATH,
                null, null);
        String body = getBody(response);
        log.info("Gateway proxy request JSON:\n" + body);
        APIGatewayV2HTTPEvent apiRequest = gson.fromJson(body, APIGatewayV2HTTPEvent.class);

        long deadlineMs = Long.parseLong(Optional.ofNullable(getHeader(response, DEADLINE_MS_HEADER))
                .orElseThrow(() -> new IllegalStateException("Lambda response missing deadline header")));
        return new LambdaInvocation(
                apiRequest,
                getHeader(response, INVOCATION_ID_HEADER),
                deadlineMs,
                getHeader(response, LAMBDA_ARN_HEADER),
                getHeader(response, XRAY_TRACE_ID_HEADER)
        );
    }

    public void postInvocationResponse(String requestId, APIGatewayV2HTTPResponse response) {
        String path = String.format(INVOCATION_RESPONSE_PATH_FORMAT, requestId);
        call(SdkHttpMethod.POST, path, null, gson.toJson(response));
    }

    public void postInvocationError(String requestId, String errorMessage, String errorType, List<String> stackTrace) {
        String path = String.format(INVOCATION_ERROR_PATH_FORMAT, requestId);
        Map<String,String> headers = Map.of(LambdaError.ERROR_TYPE_HEADER, errorType);

        LambdaError lambdaError = new LambdaError(errorMessage, errorType, stackTrace);
        call(SdkHttpMethod.POST, path, headers, gson.toJson(lambdaError));
    }

    private String getHeader(HttpExecuteResponse response, String key) {
        List<String> values = response.httpResponse().headers().get(key);
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }

    private String getBody(HttpExecuteResponse response) {
        try {
            return SdkBytes.fromByteArray(response.responseBody().orElseThrow().readAllBytes()).asUtf8String();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read HTTP response body");
        }
    }

    private HttpExecuteResponse call(SdkHttpMethod method, String path, Map<String,String> headers, String body) {
        return call(httpClientStandard, method, path, headers, body);
    }

    private HttpExecuteResponse call(SdkHttpClient client, SdkHttpMethod method, String path,
                                     Map<String,String> headers, String body) {
        String versionedPath = API_VERSION_PATH + path;
        SdkHttpRequest.Builder httpRequestBuilder = SdkHttpRequest.builder()
                .method(method)
                .protocol("http")
                .host(apiEndpointHost)
                .encodedPath(versionedPath);
        log.info("Executing method={}, host={}, path={}",
                method, httpRequestBuilder.host(), httpRequestBuilder.encodedPath());
        if (headers != null) {
            httpRequestBuilder.headers(headers.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))));
        }
        try {
            HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder()
                    .request(httpRequestBuilder.build());
            if (body != null) {
                executeRequestBuilder.contentStreamProvider(SdkBytes.fromUtf8String(body).asContentStreamProvider());
            }
            return client.prepareRequest(executeRequestBuilder.build()).call();
        } catch (IOException e) {
            throw new RuntimeException("Unable to contact Lambda Runtime API", e);
        }
    }

}