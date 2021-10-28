package com.admiralbot.framework.server;

import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.sharedutil.IDUtils;
import com.admiralbot.sharedutil.XrayUtils;

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

    /* @return Whether this class should require ApiEndpointInfo on the requested service interface.
     */
    protected abstract boolean requiresEndpointInfo();

    protected abstract String serverType();

    private boolean isInitialised = false;
    private JsonApiRequestDispatcher<ModelType> requestDispatcher;
    private ApiEndpointInfo apiEndpointInfo;
    private String serverDisplayName;

    protected void initialise() {
        // Xray defaults: treat missing context as non-fatal (only affects monitoring) and set a good service name

        String simpleServiceName = IDUtils.stripLeadingICharIfPresent(getModelClass().getSimpleName());
        XrayUtils.setServiceName(simpleServiceName);
        serverDisplayName = simpleServiceName + serverType();
        XrayUtils.setup();

        // Make a new service handler and a dispatcher for it, so subclasses can route requests

        ModelType serviceRequestHandler = createHandlerInstance();
        requestDispatcher = new JsonApiRequestDispatcher<>(serviceRequestHandler,getModelClass(), requiresEndpointInfo());
        apiEndpointInfo = getModelClass().getAnnotation(ApiEndpointInfo.class);

        isInitialised = true;
    }

    protected JsonApiRequestDispatcher<ModelType> getRequestDispatcher() {
        return requireInitialised(requestDispatcher);
    }

    protected ApiEndpointInfo getEndpointInfo() {
        return requireInitialised(apiEndpointInfo);
    }

    protected String getServerDisplayName() {
        return serverDisplayName;
    }

    private <T> T requireInitialised(T field) {
        if (!isInitialised) {
            throw new IllegalStateException("`initialize()` was not called on server instance");
        }
        return field;
    }

}