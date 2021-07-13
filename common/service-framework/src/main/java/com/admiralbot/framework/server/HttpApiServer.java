package com.admiralbot.framework.server;

import com.admiralbot.framework.common.ApiHttpMethod;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.XrayUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

public abstract class HttpApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private final Logger logger = LoggerFactory.getLogger(HttpApiServer.class);
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    public HttpApiServer() {
        super.initialise();
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
            logger.info(request.headers().toString());

            XrayUtils.beginSegment(getSimpleServiceName(), request.headers(XrayUtils.TRACE_ID_HEADER_KEY));

            try {

                XrayUtils.beginSubsegment("HandleRequest");

                String responseBody = getRequestDispatcher().handleRequest(request.body());

                logger.info("Response payload:");
                logger.info(responseBody);

                XrayUtils.endSubsegment();

                response.header("server", "Serverbot2 API");
                return responseBody;

            } catch (Exception e) {

                logger.error("Encountered error while dispatching request", e);
                XrayUtils.addSegmentException(e);

                response.status(500);

                JsonObject jsonErrorResponse = new JsonObject();
                jsonErrorResponse.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, null);
                jsonErrorResponse.addProperty(ApiConfig.JSON_RESPONSE_ERROR_KEY, "Internal error");
                return gson.toJson(jsonErrorResponse);

            } finally {

                XrayUtils.endSegment();

            }

        });
    }

}
