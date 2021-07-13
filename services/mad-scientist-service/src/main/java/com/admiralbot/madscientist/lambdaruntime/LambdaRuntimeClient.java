package com.admiralbot.madscientist.lambdaruntime;

import com.admiralbot.sharedutil.Joiner;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
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

    private static final String API_VERSION_PATH = "/2018-06-01";
    private static final String NEXT_INVOCATION_PATH = "/runtime/invocation/next";
    private static final String INIT_ERROR_PATH = "/runtime/init/error";
    private static final String INVOCATION_RESPONSE_PATH_FORMAT = "/runtime/invocation/%s/response";
    private static final String INVOCATION_ERROR_PATH_FORMAT = "/runtime/invocation/%s/error";

    private static final String API_ENDPOINT_ENV_VAR = "AWS_LAMBDA_RUNTIME_API";
    private static final Gson gson = new Gson();

    private final String apiEndpointHost;
    private final SdkHttpClient httpClientStandard;
    private final SdkHttpClient httpClientNoTimeout;

    public LambdaRuntimeClient() {
        apiEndpointHost = Objects.requireNonNull(System.getenv(API_ENDPOINT_ENV_VAR));
        httpClientNoTimeout = UrlConnectionHttpClient.builder().socketTimeout(Duration.ZERO).build();
        httpClientStandard = UrlConnectionHttpClient.builder().socketTimeout(Duration.ofSeconds(3)).build();
    }

    public void postInitError(String errorMessage, String errorType, List<String> stackTrace) {
        Map<String,String> headers = Map.of(LambdaError.ERROR_TYPE_HEADER, errorType);
        LambdaError lambdaError = LambdaError.builder()
                .errorMessage(errorMessage)
                .errorType(errorType)
                .stackTrace(stackTrace)
                .build();
        call(SdkHttpMethod.POST, INIT_ERROR_PATH, headers, gson.toJson(lambdaError));
    }

    public LambdaInvocation getNextInvocation() {
        HttpExecuteResponse response = call(httpClientNoTimeout, SdkHttpMethod.GET, NEXT_INVOCATION_PATH,
                null, null);
        String body = getBody(response);
        System.out.println(body);
        APIGatewayProxyRequestEvent apiRequest = gson.fromJson(body, APIGatewayProxyRequestEvent.class);
        long deadlineMs = Long.parseLong(Optional.ofNullable(getHeader(response, LambdaInvocation.DEADLINE_MS_HEADER))
                .orElseThrow(() -> new IllegalStateException("Lambda response missing deadline header")));
        return LambdaInvocation.builder()
                .apiGatewayRequest(apiRequest)
                .id(getHeader(response, LambdaInvocation.INVOCATION_ID_HEADER))
                .deadlineMs(deadlineMs)
                .lambdaArn(getHeader(response, LambdaInvocation.LAMBDA_ARN_HEADER))
                .xrayTraceId(getHeader(response, LambdaInvocation.XRAY_TRACE_ID_HEADER))
                .build();
    }

    public void postInvocationResponse(String requestId, APIGatewayProxyResponseEvent response) {
        String path = String.format(INVOCATION_RESPONSE_PATH_FORMAT, requestId);
        call(SdkHttpMethod.POST, path, null, gson.toJson(response));
    }

    public void postInvocationError(String requestId, String errorMessage, String errorType, List<String> stackTrace) {
        String path = String.format(INVOCATION_ERROR_PATH_FORMAT, requestId);
        Map<String,String> headers = Map.of(LambdaError.ERROR_TYPE_HEADER, errorType);
        LambdaError lambdaError = LambdaError.builder()
                .errorMessage(errorMessage)
                .errorType(errorType)
                .stackTrace(stackTrace)
                .build();
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
        System.out.println(Joiner.with("|", "host", httpRequestBuilder.host(), "path", httpRequestBuilder.encodedPath()));
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
