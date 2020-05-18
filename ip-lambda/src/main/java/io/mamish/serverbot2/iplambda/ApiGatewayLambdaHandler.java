package io.mamish.serverbot2.iplambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.exception.server.ApiServerException;
import io.mamish.serverbot2.networksecurity.model.AuthorizeIpRequest;
import io.mamish.serverbot2.networksecurity.model.INetworkSecurity;
import io.mamish.serverbot2.sharedconfig.NetSecConfig;

import java.util.Map;

public class ApiGatewayLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final INetworkSecurity networkSecurityClient = ApiClient.lambda(INetworkSecurity.class, NetSecConfig.FUNCTION_NAME);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String sourceIp = request.getRequestContext().getIdentity().getSourceIp();

        String method = request.getHttpMethod();
        if (!method.equals("GET")) {
            return generateError("Sorry, this request is invalid [bad HTTP method '" + method + "']", 400);
        }

        String encryptedUserIdToken = request.getQueryStringParameters().get("token");
        if (encryptedUserIdToken == null) {
            return generateError("Sorry, this request is invalid [missing token]", 400);
        }

        try {
            AuthorizeIpRequest authRequest = new AuthorizeIpRequest(sourceIp, encryptedUserIdToken, null);
            networkSecurityClient.authorizeIp(authRequest);
        } catch (ApiServerException e) {
            e.printStackTrace();
            return generateError("Sorry, this request is invalid [bad token]", 400);
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
            "Cache-Control", "no-cache, no-store, must-revalidate",
            "Pragma", "no-cache",
            "Expires", "0"
        );
    }

}
