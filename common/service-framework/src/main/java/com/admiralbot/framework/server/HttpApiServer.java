package com.admiralbot.framework.server;

import com.admiralbot.framework.common.ApiHttpMethod;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.TraceHeader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Spark;

public abstract class HttpApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private final Logger logger = LogManager.getLogger(HttpApiServer.class);
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    public HttpApiServer() {
        if (getEndpointInfo().httpMethod() != ApiHttpMethod.POST) {
            throw new IllegalArgumentException("HTTP APIs only support POST method currently");
        }
        // Would be nice to add this onto a 'base' path but APIGW HTTP APIs seem limited on this.
        String internalApiRequestPath = getEndpointInfo().uriPath();

        Spark.port(CommonConfig.SERVICES_INTERNAL_HTTP_PORT);
        Spark.post(internalApiRequestPath, (request, response) -> {

            logger.info("Request payload:");
            logger.info(request.body());
            logger.info("Request headers:");
            logger.info(request.headers());

            TraceHeader trace = extractTraceHeaderIfAvailable(request);
            if (trace != null) {
                AWSXRay.beginSegment(getSimpleServiceName(), trace.getRootTraceId(), trace.getParentId());
            } else {
                AWSXRay.beginSegment(getSimpleServiceName());
            }

            try {

                AWSXRay.beginSubsegment("HandleRequest");

                String responseBody = getRequestDispatcher().handleRequest(request.body());

                logger.info("Response payload:");
                logger.info(responseBody);

                AWSXRay.endSubsegment();

                response.header("server", "Serverbot2 API");
                return responseBody;

            } catch (Exception e) {

                logger.error("Encountered error while dispatching request", e);
                AWSXRay.getCurrentSegment().addException(e);

                response.status(500);

                JsonObject jsonErrorResponse = new JsonObject();
                jsonErrorResponse.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, null);
                jsonErrorResponse.addProperty(ApiConfig.JSON_RESPONSE_ERROR_KEY, "Internal error");
                return gson.toJson(jsonErrorResponse);

            } finally {

                AWSXRay.endSegment();

            }

        });
    }

    private TraceHeader extractTraceHeaderIfAvailable(Request request) {
        String traceHeaderString = request.headers(TraceHeader.HEADER_KEY);
        if (traceHeaderString != null) {
            return TraceHeader.fromString(traceHeaderString);
        }
        return null;
    }

}
