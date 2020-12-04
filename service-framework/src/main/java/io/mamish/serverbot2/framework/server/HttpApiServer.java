package io.mamish.serverbot2.framework.server;

import spark.Spark;

public abstract class HttpApiServer<ModelType> extends AbstractApiServer<ModelType> {

    public HttpApiServer() {
        Spark.post("/", (request, response) -> {
            return getRequestDispatcher().handleRequest(request.body());
        });
    }

}
