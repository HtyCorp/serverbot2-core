package io.mamish.serverbot2.lambdawarmer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.LambdaWarmerConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.ServiceException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ScheduledLambdaHandler implements RequestHandler<ScheduledEvent,String> {

    private final Logger logger = LogManager.getLogger(ScheduledLambdaHandler.class);

    private final LambdaClient lambdaClient = LambdaClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(new SystemSettingsRegionProvider().getRegion())
            .build();

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {

        for (String functionName: LambdaWarmerConfig.FUNCTION_NAMES_TO_WARM) {

            String aliasName = IDUtils.colon(functionName, CommonConfig.LAMBDA_LIVE_ALIAS_NAME);
            SdkBytes pingPayload = SdkBytes.fromUtf8String(LambdaWarmerConfig.LAMBDA_WARMER_PING_STRING);
            try {
                Instant invokeStartTime = Instant.now();
                lambdaClient.invoke(r -> r.functionName(aliasName).payload(pingPayload));
                long invocationTimeMs = invokeStartTime.until(Instant.now(), ChronoUnit.MILLIS);
                logger.info("Ping invocation for {} took {}ms", functionName, invocationTimeMs);
            } catch (ServiceException e) {
                logger.warn("Error while invoking function " + functionName, e);
            }

        }

        return "Done!";
    }



}
