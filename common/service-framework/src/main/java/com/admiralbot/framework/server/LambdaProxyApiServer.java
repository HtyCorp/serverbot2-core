package com.admiralbot.framework.server;

import com.admiralbot.framework.exception.server.FrameworkInternalException;
import com.admiralbot.framework.server.lambdaruntime.LambdaInvocation;
import com.admiralbot.framework.server.lambdaruntime.LambdaRuntimeClient;
import com.admiralbot.sharedutil.Joiner;
import com.admiralbot.sharedutil.XrayUtils;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

public abstract class LambdaProxyApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private static final Map<String,String> STANDARD_RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Server", "AdmiralbotCustomLambdaProxy"
    );

    private static final Logger logger = LoggerFactory.getLogger(LambdaProxyApiServer.class);

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    private final LambdaRuntimeClient runtimeClient;
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    public LambdaProxyApiServer() {
        runtimeClient = new LambdaRuntimeClient();
        try {
            super.initialise();
            (new Thread(this::lambdaInvocationHandlerLoop, "LambdaInvocationHandlerLoop")).start();
        } catch (Exception e) {
            String errorMessage = "LambdaRuntimeServer initialization failed: " + e.getMessage();
            runtimeClient.postInitError(errorMessage, e.getClass().getSimpleName(), null);
            throw new RuntimeException(errorMessage, e);
        }
    }

    private void lambdaInvocationHandlerLoop() {
        while(true) {
            LambdaInvocation invocation = runtimeClient.getNextInvocation();
            try {
                APIGatewayV2HTTPResponse responseEvent = handleInvocation(invocation);
                runtimeClient.postInvocationResponse(invocation.getId(), responseEvent);
            } catch (Exception invokeException) {
                logger.error("Internal error during invocation handling", invokeException);
                runtimeClient.postInvocationError(invocation.getId(), invokeException.toString(),
                        invokeException.getClass().getSimpleName(), null);
            }
        }
    }

    private APIGatewayV2HTTPResponse handleInvocation(LambdaInvocation invocation) {
        long maxExecutionTimeMs = invocation.getDeadlineMs() - Instant.now().toEpochMilli();

        APIGatewayV2HTTPResponse errorResponse = generateErrorIfInvalidRequest(invocation.getApiGatewayEvent());
        if (errorResponse != null) {
            return errorResponse;
        }

        if (invocation.getXrayTraceId() != null) {
            XrayUtils.setTraceId(invocation.getXrayTraceId());
        }
        Future<String> responseFuture = threadExecutor.submit(() ->
                getRequestDispatcher().handleRequest(invocation.getApiGatewayEvent().getBody()));

        try {
            String responseBody = responseFuture.get(maxExecutionTimeMs, TimeUnit.MILLISECONDS);
            return standardResponse(200, responseBody);
        } catch (ExecutionException e) {
            throw new FrameworkInternalException("Invocation encountered an error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new FrameworkInternalException("Invocation interrupted", e);
        } catch (TimeoutException e) {
            throw new FrameworkInternalException("Task timed out");
        } finally {
            responseFuture.cancel(true);
        }

    }

    private APIGatewayV2HTTPResponse generateErrorIfInvalidRequest(APIGatewayV2HTTPEvent request) {
        String apiExpectedHttpMethod = getEndpointInfo().httpMethod().toString();
        if (!apiExpectedHttpMethod.equalsIgnoreCase(request.getRequestContext().getHttp().getMethod())) {
            return standardResponse(405, "{\"message\":\"Method not allowed\"}");
        }
        // The path reported by APIGW v2 includes the stage prefix, e.g. an API configured with serviceName="example"
        // and uriPath="/" would expect the APIGW-reported path to be "/example/".
        // The extra slug prefix is added by the APIGW's custom domain feature on a normal "/" customer request.
        String apiPathWithServiceName = Joiner.slash("",
                getEndpointInfo().serviceName(), getEndpointInfo().uriPath());
        if (!apiPathWithServiceName.equalsIgnoreCase(request.getRequestContext().getHttp().getPath())) {
            return standardResponse(404, "{\"message\":\"Not found\"}");
        }
        return null;
    }

    private APIGatewayV2HTTPResponse standardResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(STANDARD_RESPONSE_HEADERS)
                .withBody(body)
                .build();
    }

}