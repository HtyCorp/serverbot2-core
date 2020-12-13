package io.mamish.serverbot2.iplambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.exception.server.ResourceExpiredException;
import io.mamish.serverbot2.networksecurity.model.AuthorizeIpRequest;
import io.mamish.serverbot2.networksecurity.model.GetAuthorizationByIpRequest;
import io.mamish.serverbot2.networksecurity.model.GetAuthorizationByIpResponse;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.LambdaWarmerConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import io.mamish.serverbot2.sharedutil.LogUtils;
import io.mamish.serverbot2.sharedutil.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApiGatewayLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Used to choose the time unit displayed for time until expiry in /check handler.
    // Uses a list-of-pairs to make it clear that insertion/iteration order matters.
    private static final List<Pair<Integer,ChronoUnit>> DISPLAY_TIME_UNIT_THRESHOLDS = List.of(
            new Pair<>(3, ChronoUnit.DAYS),
            new Pair<>(2, ChronoUnit.HOURS),
            new Pair<>(0, ChronoUnit.MINUTES)
    );

    private final Logger logger = LogManager.getLogger(ApiGatewayLambdaHandler.class);
    private final Gson gson = new Gson();

    private final INetworkSecurity networkSecurityClient = ApiClient.http(INetworkSecurity.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String path = request.getPath();

        // Check if this a ping request from the warmer and exit early if so
        if (path.equals(LambdaWarmerConfig.WARMER_PING_API_PATH)) {
            logger.info("Warmer ping request");
            return generateSuccess("Ping okay");
        }

        logger.info("Dumping request payload:\n" + gson.toJson(request));

        String method = request.getHttpMethod();
        if (!method.equals("GET")) {
            logger.error("Invalid request: bad HTTP method {}", method);
            return generateError("Sorry, this request is invalid [bad HTTP method '" + method + "']", 400);
        }

        String sourceIpAddress = request.getRequestContext().getIdentity().getSourceIp();
        Map<String,String> queryParams = Optional.ofNullable(request.getQueryStringParameters()).orElse(Map.of());

        if (path.equals(NetSecConfig.AUTHORIZER_PATH_AUTHORIZE)) {
            return handleAuthorize(sourceIpAddress, queryParams);
        } else if (path.equals(NetSecConfig.AUTHORIZER_PATH_CHECK)) {
            return handleCheck(sourceIpAddress);
        } else {
            logger.error("Invalid request: path '{}' not found", path);
            return generateError("Invalid request: path not found", 404);
        }
    }

    private APIGatewayProxyResponseEvent handleAuthorize(String sourceIpAddress, Map<String,String> queryParameters) {

        String encryptedAuthToken = queryParameters.get(NetSecConfig.AUTHORIZER_PATH_PARAM_TOKEN);
        if (encryptedAuthToken == null) {
            logger.error("Invalid request: missing auth token");
            return generateError("Sorry, this request is invalid [missing token]", 400);
        }

        try {
            AuthorizeIpRequest authRequest = new AuthorizeIpRequest(sourceIpAddress, encryptedAuthToken);
            networkSecurityClient.authorizeIp(authRequest);
        } catch (RequestValidationException e) {
            logger.error("NetSec AuthorizeIp validation failed", e);
            return generateError("Invalid token. Check that you've used the exact URL sent in Discord.", 400);
        } catch (ResourceExpiredException e) {
            logger.error("NetSec AuthorizeIp failed due to token expiration", e);
            return generateError("Sorry, this link has expired. Try getting a new one from wherever you got this link", 403);
        } catch (ApiServerException e) {
            logger.error("NetSec AuthorizeIp general error", e);
            return generateError("Sorry, something went wrong.", 500);
        }

        logger.info("Successful whitelist for source IP {}", sourceIpAddress);
        return generateSuccess("Thank you. Your IP address (" + sourceIpAddress + ") has been whitelisted to connect to servers.");

    }

    private APIGatewayProxyResponseEvent handleCheck(String sourceIpAddress) {

        GetAuthorizationByIpResponse authorizationResponse = networkSecurityClient.getAuthorizationByIp(
                new GetAuthorizationByIpRequest(sourceIpAddress)
        );
        LogUtils.infoDump(logger, "NetSec GetAuthorizationByIp response:", authorizationResponse);

        String friendlyDomainName = CommonConfig.APP_ROOT_DOMAIN_NAME.getValue();

        if (authorizationResponse.isAuthorized()) {
            Instant expiryTime = Instant.ofEpochSecond(authorizationResponse.getExpiryTimeEpochSeconds());
            String expiryTimeUntilString = displayTimeUntil(expiryTime);
            return generateSuccess(
                    "Your IP address ("+sourceIpAddress+") is whitelisted to join "+friendlyDomainName+" servers.\n"
                    + "This will expire " + expiryTimeUntilString + "."
            );
        } else {
            return generateSuccess(
                    "Your IP address ("+sourceIpAddress+") is not whitelisted to join "+friendlyDomainName+" servers.\n"
                    + "Use !addip in any server bot channel to whitelist it."
            );
        }

    }

    private String displayTimeUntil(Instant endInstant) {
        Instant now = Instant.now();

        if (now.isAfter(endInstant)) {
            return "very soon";
        }

        for (Pair<Integer,ChronoUnit> thresholdEntry: DISPLAY_TIME_UNIT_THRESHOLDS) {
            int threshold = thresholdEntry.a();
            ChronoUnit timeUnit = thresholdEntry.b();
            long timeUntilInThisUnit = now.until(endInstant, timeUnit);
            if (timeUntilInThisUnit >= threshold) {
                return "in " + timeUntilInThisUnit + " " + timeUnit.toString().toLowerCase();
            }
        }

        return "very soon";
    }

    private APIGatewayProxyResponseEvent generateSuccess(String message) {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(noCacheHeaders())
                .withStatusCode(200)
                .withBody(message);
    }

    private APIGatewayProxyResponseEvent generateError(String message, Integer code) {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(noCacheHeaders())
                .withStatusCode(code)
                .withBody(message);
    }

    private Map<String,String> noCacheHeaders() {
        // Ref: https://stackoverflow.com/questions/49547/how-do-we-control-web-page-caching-across-all-browsers
        return Map.of(
            "Content-Type", "text/plain; charset=UTF-8",
            "Cache-Control", "no-cache, no-store, must-revalidate",
            "Pragma", "no-cache",
            "Expires", "0"
        );
    }

}
