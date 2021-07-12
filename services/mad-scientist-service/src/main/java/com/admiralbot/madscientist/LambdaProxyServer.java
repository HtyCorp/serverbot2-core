package com.admiralbot.madscientist;

import com.admiralbot.framework.exception.server.FrameworkInternalException;
import com.admiralbot.framework.server.AbstractApiServer;
import com.amazonaws.services.lambda.runtime.api.client.runtimeapi.InvocationRequest;
import com.amazonaws.services.lambda.runtime.api.client.runtimeapi.LambdaRuntimeClient;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.entities.TraceHeader;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

public abstract class LambdaProxyServer<ModelType> extends AbstractApiServer<ModelType> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaProxyServer.class);

    private final Gson gson = new Gson();

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    private final LambdaRuntimeClient runtimeClient;
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    public LambdaProxyServer() {
        String runtimeApiEndpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
        runtimeClient = new LambdaRuntimeClient(runtimeApiEndpoint);
        try {
            super.initialise();
            (new Thread(this::lambdaInvocationHandlerLoop, "LambdaInvocationHandlerLoop")).start();
        } catch (Exception e) {
            String errorMessage = "LambdaRuntimeServer initialization failed: " + e.getMessage();
            try {
                runtimeClient.postInitError(SdkBytes.fromUtf8String(errorMessage).asByteArray(),
                        "LambdaRuntimeServer.Initialise");
            } catch (IOException ioe) {
                logger.error("Couldn't post Lambda init error", ioe);
            }
            throw new RuntimeException(errorMessage, e);
        }
    }

    private void lambdaInvocationHandlerLoop() {
        while(true) {
            InvocationRequest invocation = runtimeClient.waitForNextInvocation();
            try {
                APIGatewayProxyResponseEvent responseEvent = handleInvocation(invocation);
                String responseString = gson.toJson(responseEvent);
                runtimeClient.postInvocationResponse(invocation.getId(),
                        SdkBytes.fromUtf8String(responseString).asByteArray());
            } catch (Exception invokeException) {
                logger.error("Internal error during invocation handling", invokeException);
                try {
                    runtimeClient.postInvocationError(invocation.getId(),
                            SdkBytes.fromUtf8String(invokeException.toString()).asByteArray(),
                            "LambdRuntimeServer.InvocationUnknown");
                } catch (IOException postErrorException) {
                    logger.error("Failed to post invocation error", postErrorException);
                }
            }
        }
    }

    private APIGatewayProxyResponseEvent handleInvocation(InvocationRequest invocation) {
        String proxyRequestString = SdkBytes.fromInputStream(invocation.getContentAsStream()).asUtf8String();
        APIGatewayProxyRequestEvent proxyRequest = gson.fromJson(proxyRequestString, APIGatewayProxyRequestEvent.class);
        long maxExecutionTimeMs = invocation.getDeadlineTimeInMs() - Instant.now().toEpochMilli();

        APIGatewayProxyResponseEvent errorResponse = generateErrorIfInvalidRequest(proxyRequest);
        if (errorResponse != null) {
            return errorResponse;
        }

        if (invocation.getXrayTraceId() != null) {
            // The ideal is to modify the equivalent env var, but modifying env vars in Java is not simple
            // Recent versions of AWS Xray Java SDK support this system property as an alternative
            System.setProperty("com.amazonaws.xray.traceHeader", invocation.getXrayTraceId());
        }
        Future<String> responseFuture = threadExecutor.submit(() ->
                getRequestDispatcher().handleRequest(proxyRequest.getBody()));

        try {
            String responseBody = responseFuture.get(maxExecutionTimeMs, TimeUnit.MILLISECONDS);
            return standardResponse(200)
                    .withBody(responseBody);
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

    private TraceHeader extractTraceHeader(InvocationRequest invocation) {
        if (invocation.getXrayTraceId() != null) {
            return TraceHeader.fromString(invocation.getXrayTraceId());
        }
        return null;
    }

    private APIGatewayProxyResponseEvent generateErrorIfInvalidRequest(APIGatewayProxyRequestEvent request) {
        if (!request.getHttpMethod().equalsIgnoreCase(getEndpointInfo().httpMethod().toString())) {
            return standardResponse(405).withBody("{\"message\":\"Method not allowed\"}");
        }
        if (!request.getPath().equalsIgnoreCase(getEndpointInfo().uriPath())) {
            return standardResponse(404).withBody("{\"message\":\"Not found\"}");
        }
        return null;
    }

    private APIGatewayProxyResponseEvent standardResponse(int statusCode) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Server", "AdmiralNativeLambdaProxy"));
    }

}
