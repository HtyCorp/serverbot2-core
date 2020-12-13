package io.mamish.serverbot2.framework.server;

import com.google.gson.Gson;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

public abstract class HttpApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private final Logger logger = LogManager.getLogger(HttpApiServer.class);
    private final Gson gson = new Gson();

    public HttpApiServer() {
        if (getEndpointInfo().httpMethod() != ApiHttpMethod.POST) {
            throw new IllegalArgumentException("HTTP APIs only support POST method currently");
        }
        String internalApiRequestPath = ApiConfig.REQUEST_INTERNAL_BASE_PATH + getEndpointInfo().uriPath();

        Spark.port(CommonConfig.SERVICES_INTERNAL_HTTP_PORT);
        Spark.post(internalApiRequestPath, (request, response) -> {

            logger.info("Request payload:");
            logger.info(gson.toJson(request));

            String responseBody = getRequestDispatcher().handleRequest(request.body());

            logger.info("Response payload:");
            logger.info(responseBody);

            return responseBody;

        });
    }

}
