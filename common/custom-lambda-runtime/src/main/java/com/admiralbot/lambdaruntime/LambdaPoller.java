package com.admiralbot.lambdaruntime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Function;

public class LambdaPoller<BodyType,ResponseType> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaPoller.class);

    private final ExecutorService threadExecutor;
    private final LambdaRuntimeClient<BodyType,ResponseType> runtimeClient;
    private final Function<BodyType,ResponseType> invocationHandler;

    public LambdaPoller(Class<BodyType> requestBodyClass, Function<BodyType,ResponseType> invocationHandler) {
        this.threadExecutor = Executors.newSingleThreadExecutor();
        this.runtimeClient = new LambdaRuntimeClient<>(requestBodyClass);
        this.invocationHandler = invocationHandler;

        try {
            (new Thread(this::lambdaInvocationHandlerLoop, "LambdaInvocationHandlerLoop")).start();
        } catch (Exception e) {
            String errorMessage = "LambdaRuntimeServer initialization failed: " + e.getMessage();
            runtimeClient.postInitError(errorMessage, e.getClass().getSimpleName(), null);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public void postInitError(String errorMessage, String errorType) {
        runtimeClient.postInitError(errorMessage, errorType, null);
    }

    private void lambdaInvocationHandlerLoop() {
        // TODO: Rely on Lambda active tracing once HTTP APIs support Xray integration
        while(true) {
            LambdaInvocation<BodyType> invocation = runtimeClient.getNextInvocation();

            ResponseType response = null;
            Exception invokeException = null;

            long maxExecutionTimeMs = invocation.getDeadlineMs() - Instant.now().toEpochMilli();
            Future<ResponseType> responseFuture = threadExecutor.submit(() -> invocationHandler.apply(invocation.getRequestBody()));

            try {
                response = responseFuture.get(maxExecutionTimeMs, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                invokeException = (Exception) e.getCause();
            } catch (Exception e) {
                invokeException = e;
            }

            if (invokeException == null) {
                runtimeClient.postInvocationResponse(invocation.getId(), response);
            } else {
                logger.error("Lambda invocation {} failed", invocation.getId(), invokeException);
                tryPostError(invocation.getId(), invokeException);
            }
        }
    }

    private void tryPostError(String invocationId, Exception exception) {
        // This API POST is the only way we have to report error; if it *also* fails, all we can do is ignore and move on.
        try {
            runtimeClient.postInvocationError(invocationId, exception.toString(),
                    exception.getClass().getCanonicalName(), null);
        } catch (Exception e) {
            logger.error("Failed to post error to Lambda runtime API", e);
        }
    }
}
