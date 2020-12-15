package io.mamish.serverbot2.framework.server;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.TraceHeader;
import io.mamish.serverbot2.framework.common.ApiHttpMethod;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Spark;

public abstract class HttpApiServer<ModelType> extends AbstractApiServer<ModelType> {

    private final Logger logger = LogManager.getLogger(HttpApiServer.class);

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
                AWSXRay.beginSegment("HandleRequest", trace.getRootTraceId(), trace.getParentId());
            } else {
                AWSXRay.beginSegment("HandleRequest");
            }

            try {

                String responseBody = getRequestDispatcher().handleRequest(request.body());

                logger.info("Response payload:");
                logger.info(responseBody);

                return responseBody;

            } catch (Exception e) {

                logger.error("Encountered error while dispatching request", e);
                AWSXRay.getCurrentSegment().addException(e);

                response.status(500);
                return "Internal server error";

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
