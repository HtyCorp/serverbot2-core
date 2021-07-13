package com.admiralbot.madscientist;

import com.admiralbot.framework.exception.server.FrameworkInternalException;
import com.admiralbot.framework.server.AbstractApiServer;
import com.admiralbot.madscientist.lambdaruntime.LambdaInvocation;
import com.admiralbot.madscientist.lambdaruntime.LambdaRuntimeClient;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                APIGatewayProxyResponseEvent responseEvent = handleInvocation(invocation);
                runtimeClient.postInvocationResponse(invocation.getId(), responseEvent);
            } catch (Exception invokeException) {
                logger.error("Internal error during invocation handling", invokeException);
                runtimeClient.postInvocationError(invocation.getId(), invokeException.toString(),
                        invokeException.getClass().getSimpleName(), null);
            }
        }
    }

    private APIGatewayProxyResponseEvent handleInvocation(LambdaInvocation invocation) {
        long maxExecutionTimeMs = invocation.getDeadlineMs() - Instant.now().toEpochMilli();

        APIGatewayProxyResponseEvent errorResponse = generateErrorIfInvalidRequest(invocation.getApiGatewayRequest());
        if (errorResponse != null) {
            return errorResponse;
        }

        if (invocation.getXrayTraceId() != null) {
            // The ideal is to modify the equivalent env var, but modifying env vars in Java is not simple
            // Recent versions of AWS Xray Java SDK support this system property as an alternative
            System.setProperty("com.amazonaws.xray.traceHeader", invocation.getXrayTraceId());
        }
        Future<String> responseFuture = threadExecutor.submit(() ->
                getRequestDispatcher().handleRequest(invocation.getApiGatewayRequest().getBody()));

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

    private APIGatewayProxyResponseEvent generateErrorIfInvalidRequest(APIGatewayProxyRequestEvent request) {
        System.out.println("request="+request);
        System.out.println("endpointInfo="+getEndpointInfo());
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
