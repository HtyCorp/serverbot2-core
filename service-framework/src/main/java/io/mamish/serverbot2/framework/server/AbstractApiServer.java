package io.mamish.serverbot2.framework.server;

import io.mamish.serverbot2.framework.common.ApiEndpointInfo;

public abstract class AbstractApiServer<ModelType> {

    /**
     * <p>
     * Must return the class of generic parameter <code>ModelType</code>.
     * <p>
     * Due to type erasure, the class corresponding to a given generic parameter can't be retrieved dynamically at
     * runtime, so it needs to be explicitly provided by the subclass.
     *
     * @return The class of generic parameter <code>ModelType</code>
     */
    protected abstract Class<ModelType> getModelClass();

    /**
     * <p>
     * Create a new instance of <code>ModelType</code> to handle API requests for the given service model.
     * <p>
     * Warning: this is called during the super constructor in LambdaApiServer, which runs <b>before</b> any instance
     * field initialization in the subclass. You cannot refer to any instance fields since they will be null at this
     * point.
     * <p>
     * This class will attempt to parse the payload of Lambda invocations as requests in the given service, and dispatch
     * them to the provided handler.
     *
     * @return An instance of <code>ModelType</code> to handle API requests
     */
    protected abstract ModelType createHandlerInstance();

    private final JsonApiRequestDispatcher<ModelType> requestDispatcher = new JsonApiRequestDispatcher<>(createHandlerInstance(),getModelClass());
    private final ApiEndpointInfo apiEndpointInfo = getModelClass().getAnnotation(ApiEndpointInfo.class);

    public JsonApiRequestDispatcher<ModelType> getRequestDispatcher() {
        return requestDispatcher;
    }

    public ApiEndpointInfo getEndpointInfo() {
        return apiEndpointInfo;
    }
}
