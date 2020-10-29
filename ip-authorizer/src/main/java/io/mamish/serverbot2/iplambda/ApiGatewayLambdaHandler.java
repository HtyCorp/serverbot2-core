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
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.LambdaWarmerConfig;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class ApiGatewayLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Logger logger = LogManager.getLogger(ApiGatewayLambdaHandler.class);
    private final Gson gson = new Gson();

    private final INetworkSecurity networkSecurityClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String path = request.getPath();
        String sourceIp = request.getRequestContext().getIdentity().getSourceIp();

        // Check if this a ping request from the warmer and exit early if so
        if (path.equals(LambdaWarmerConfig.WARMER_PING_API_PATH)) {
            logger.info("Warmer ping request");
            return generateSuccess("Ping okay");
        }

        logger.info("Dumping request object:\n" + gson.toJson(request));

        if (!path.equals(NetSecConfig.AUTH_PATH)) {
            return generateError("Sorry, this request is invalid [bad path '" + path + "']", 400);
        }

        String method = request.getHttpMethod();
        if (!method.equals("GET")) {
            return generateError("Sorry, this request is invalid [bad HTTP method '" + method + "']", 400);
        }

        // This map is actually null rather than just empty if no params are provided
        // Probably due to the specifics of the Lambda runtime parser
        Map<String,String> queryParams = request.getQueryStringParameters();
        if (queryParams == null) {
            return generateError("Sorry, this request is invalid [missing token]", 400);
        }

        String encryptedAuthToken = queryParams.get(NetSecConfig.AUTH_PARAM_TOKEN);
        if (encryptedAuthToken == null) {
            return generateError("Sorry, this request is invalid [missing token]", 400);
        }

        try {
            AuthorizeIpRequest authRequest = new AuthorizeIpRequest(sourceIp, encryptedAuthToken);
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

        return generateSuccess("Thank you. Your IP address (" + sourceIp + ") has been whitelisted to connect to servers.");

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
