package com.admiralbot.framework.server;

import com.admiralbot.lambdaruntime.LambdaPoller;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.XrayUtils;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;

import java.util.Base64;
import java.util.Map;

public abstract class LambdaProxyApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private static final Map<String,String> STANDARD_RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Server", "AdmiralbotCustomLambdaProxy"
    );

    private static final Logger logger = LoggerFactory.getLogger(LambdaProxyApiServer.class);

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    @Override
    protected String serverType() {
        return "LambdaProxy";
    }

    private final LambdaPoller<APIGatewayV2HTTPEvent,APIGatewayV2HTTPResponse> lambdaPoller;

    public LambdaProxyApiServer() {
        lambdaPoller = new LambdaPoller<>(APIGatewayV2HTTPEvent.class, this::handleRequestWithXray);
        try {
            super.initialise();
        } catch (Exception e) {
            String errorMessage = "LambdaRuntimeServer initialization failed: " + e.getMessage();
            lambdaPoller.postInitError(errorMessage, e.getClass().getSimpleName());
            throw new RuntimeException(errorMessage, e);
        }
    }

    private APIGatewayV2HTTPResponse handleRequestWithXray(APIGatewayV2HTTPEvent request) {
        String traceHeaderString = request.getHeaders()
                .get(XrayUtils.TRACE_HEADER_HTTP_HEADER_KEY.toLowerCase());
        XrayUtils.beginSegment(getServerDisplayName(), traceHeaderString);
        try {
            return handleInvocation(request);
        } catch (RuntimeException e) {
            XrayUtils.addSegmentException(e);
            throw e;
        } finally {
            XrayUtils.endSegment();
        }
    }

    private APIGatewayV2HTTPResponse handleInvocation(APIGatewayV2HTTPEvent request) {
        APIGatewayV2HTTPResponse errorResponse = generateErrorIfInvalidRequest(request);
        if (errorResponse != null) {
            return errorResponse;
        }

        String requestBody = getRequestBody(request);
        LogUtils.info(logger, () -> "Service request JSON:\n" + requestBody);

        String serviceResponseJson = getRequestDispatcher().handleRequest(requestBody);
        LogUtils.info(logger, () -> "Service response JSON:\n" + serviceResponseJson);
        return standardResponse(200, serviceResponseJson);
    }

    private APIGatewayV2HTTPResponse generateErrorIfInvalidRequest(APIGatewayV2HTTPEvent request) {
        String apiExpectedHttpMethod = getEndpointInfo().httpMethod().toString();
        if (!apiExpectedHttpMethod.equalsIgnoreCase(request.getRequestContext().getHttp().getMethod())) {
            return standardResponse(405, "{\"message\":\"Method not allowed\"}");
        }
        // The path reported by APIGW v2 includes the stage prefix, e.g. an API configured with serviceName="example"
        // and uriPath="/" would expect the APIGW-reported path to be "/example/".
        // The extra slug prefix is added by the APIGW's custom domain feature on a normal "/" customer request.
        String apiPathWithServiceName = "/" + getEndpointInfo().serviceName() + getEndpointInfo().uriPath();
        if (!apiPathWithServiceName.equalsIgnoreCase(request.getRequestContext().getHttp().getPath())) {
            return standardResponse(404, "{\"message\":\"Not found\"}");
        }
        return null;
    }

    private static String getRequestBody(APIGatewayV2HTTPEvent request) {
        if (request.getIsBase64Encoded()) {
            String contentType = request.getHeaders().get("content-type");
            logger.info("Decoding base64-encoded request body (for Content-Type=" + contentType + ")");
            return SdkBytes.fromByteArray(Base64.getDecoder().decode(request.getBody())).asUtf8String();
        } else {
            return request.getBody();
        }
    }

    private APIGatewayV2HTTPResponse standardResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(STANDARD_RESPONSE_HEADERS)
                .withBody(body)
                .build();
    }

}