package io.mamish.serverbot2.iplambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApiGatewayLambdaHandlerTest {

    @Test
    public void testDummyResponse() {
        ApiGatewayLambdaHandler handler = new ApiGatewayLambdaHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/")
                .withBody("Some body content")
                .withHttpMethod("GET");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("Hello, I haven't been implemented yet. Bye!", response.getBody());
    }

}
