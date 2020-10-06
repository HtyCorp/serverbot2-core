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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ScheduledLambdaHandler implements RequestHandler<ScheduledEvent,String> {

    private final Logger logger = LogManager.getLogger(ScheduledLambdaHandler.class);

    private final LambdaClient lambdaClient = LambdaClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(new SystemSettingsRegionProvider().getRegion())
            .build();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {

        for (String functionName: LambdaWarmerConfig.FUNCTION_NAMES_TO_WARM) {

            String aliasName = IDUtils.colon(functionName, CommonConfig.LAMBDA_LIVE_ALIAS_NAME);
            SdkBytes pingPayload = SdkBytes.fromUtf8String(LambdaWarmerConfig.WARMER_PING_LAMBDA_PAYLOAD);
            try {
                Instant invokeStartTime = Instant.now();
                lambdaClient.invoke(r -> r.functionName(aliasName).payload(pingPayload));
                long invocationTimeMs = invokeStartTime.until(Instant.now(), ChronoUnit.MILLIS);
                logger.info("Ping invocation for {} took {}ms", functionName, invocationTimeMs);
            } catch (ServiceException e) {
                logger.warn("Error while invoking function " + functionName, e);
            }

        }

        for (String apiSubdomain: LambdaWarmerConfig.API_SUBDOMAINS_TO_WARM) {

            URI targetApiUri = URI.create("https://"
                    + apiSubdomain
                    + "."
                    + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                    + LambdaWarmerConfig.WARMER_PING_API_PATH);
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(targetApiUri)
                    .build();
            try {
                Instant requestStartTime = Instant.now();
                int statusCode = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                long requestTimeMs = requestStartTime.until(Instant.now(), ChronoUnit.MILLIS);
                logger.info("HTTP invocation for {} took {}ms with status code {}",
                        targetApiUri, requestTimeMs, statusCode);
            } catch (IOException e) {
                logger.warn("IOException while invoking API " + targetApiUri, e);
            } catch (InterruptedException e) {
                logger.warn("InterruptedException while invoking API " + targetApiUri, e);
            }

        }

        return "Done!";
    }



}
