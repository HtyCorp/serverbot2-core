package com.admiralbot.ipauth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class ApiGatewayLambdaHandlerTest {

// Tests disabled until I figure out how to mock various APIs for this

//    @Test
//    public void testBadHttpMethod() {
//        var response = generateResponse(new APIGatewayProxyRequestEvent()
//            .withHttpMethod("POST")
//            .withPath("/")
//            .withBody("\"some\":\"body\"}")
//            .withQueryStringParameters(Map.of("token", "dummyToken"))
//        );
//        Assertions.assertEquals(400, response.getStatusCode());
//        Assertions.assertEquals("Sorry, this request is invalid [bad HTTP method 'POST']", response.getBody());
//    }
//
//    @Test
//    public void testMissingTokenParam() {
//        var response = generateResponse(new APIGatewayProxyRequestEvent()
//                .withHttpMethod("GET")
//                .withPath("/")
//        );
//        Assertions.assertEquals(400, response.getStatusCode());
//        Assertions.assertEquals("Sorry, this request is invalid [missing token]", response.getBody());
//    }

    private APIGatewayProxyResponseEvent generateResponse(APIGatewayProxyRequestEvent request) {
        ApiGatewayLambdaHandler handler = new ApiGatewayLambdaHandler();
        return handler.handleRequest(request, null);
    }

}
