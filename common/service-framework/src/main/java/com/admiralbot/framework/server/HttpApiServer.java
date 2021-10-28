package com.admiralbot.framework.server;

import com.admiralbot.framework.modelling.ApiHttpMethod;
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

    private static final String SERVER_HEADER_VALUE = "AdmiralbotHttpProxy";

    private final Logger logger = LoggerFactory.getLogger(HttpApiServer.class);
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    protected boolean requiresEndpointInfo() {
        return true;
    }

    @Override
    protected String serverType() {
        return "HttpServer";
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

            String traceHeader = request.headers(XrayUtils.TRACE_HEADER_HTTP_HEADER_KEY);

            try {

                XrayUtils.beginSegment(getServerDisplayName(), traceHeader);

                logger.info("Request payload:");
                logger.info(request.body());
                logger.info("Request headers:");
                logger.info(request.headers().toString());

                response.type("application/json");
                response.header("Server", SERVER_HEADER_VALUE);

                String responseBody = XrayUtils.subsegment("HandleRequest", null,
                        () -> getRequestDispatcher().handleRequest(request.body()));

                logger.info("Response payload:");
                logger.info(responseBody);

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