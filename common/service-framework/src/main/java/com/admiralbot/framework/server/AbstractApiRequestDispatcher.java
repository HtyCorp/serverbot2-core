package com.admiralbot.framework.server;

import com.admiralbot.framework.common.ApiActionDefinition;
import com.admiralbot.framework.common.ApiDefinitionSet;
import com.admiralbot.framework.exception.ApiException;
import com.admiralbot.framework.exception.server.*;
import com.admiralbot.sharedutil.Pair;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractApiRequestDispatcher<ModelType, OutputType, RawInputType, ParsedInputType> {

    private final ModelType handlerInstance;
    private final ApiDefinitionSet<ModelType> apiDefinitionSet;

    private final Logger logger = LoggerFactory.getLogger(AbstractApiRequestDispatcher.class);

    private AbstractApiRequestDispatcher<?, OutputType, RawInputType, ?> nextChainDispatcher;

    public AbstractApiRequestDispatcher(ModelType handlerInstance, Class<ModelType> handlerInterfaceClass,
                                        boolean requiresEndpointInfo) {
        this.handlerInstance = handlerInstance;
        logger.trace("Building definition set for " + handlerInterfaceClass.getSimpleName());
        this.apiDefinitionSet = new ApiDefinitionSet<>(handlerInterfaceClass, requiresEndpointInfo);
        logger.trace("Finished construction");
    }

    public ApiDefinitionSet<ModelType> getApiDefinitionSet() {
        return apiDefinitionSet;
    }

    protected abstract Pair<String, ParsedInputType> parseNameKey(RawInputType rawInput)
            throws UnparsableInputException;

    // return type unspecified since its unique to each request definition
    protected abstract Object parseRequestObject(ApiActionDefinition definition, ParsedInputType parsedInput)
            throws UnparsableInputException, RequestValidationException;

    protected abstract OutputType serializeResponseObject(ApiActionDefinition definition, Object handlerResult)
            throws SerializationException;

    protected abstract OutputType serializeErrorObject(ApiServerException exception);

    public final void setNextChainDispatcher(AbstractApiRequestDispatcher<?,OutputType,RawInputType,?> nextChainDispatcher) {
        this.nextChainDispatcher = nextChainDispatcher;
    }

    public final OutputType handleRequest(RawInputType rawInput) {
        try {
            return internalHandleRequest(rawInput);
        } catch (ApiServerException e) {
            logger.error("Standard exception type in API request dispatcher", e);
            return serializeErrorObject(e);
        } catch (Exception e) {
            String message = "Non-standard unexpected exception in API request dispatcher";
            logger.error(message, e);
            return serializeErrorObject(new FrameworkInternalException(message));
        }
    }

    private OutputType internalHandleRequest(RawInputType rawInput) throws ApiException {

        Pair<String, ParsedInputType> nameAndRemainingInput = parseNameKey(rawInput);
        logger.trace("Parsed name key as: " + nameAndRemainingInput.a());

        String targetName = nameAndRemainingInput.a();
        ParsedInputType parsedInput = nameAndRemainingInput.b();
        ApiActionDefinition definition = apiDefinitionSet.getFromName(targetName);

        // If this dispatcher doesn't have any such request definition but is chained to another dispatch,
        // invoke that dispatcher to see if it can get a result.
        if (definition == null) {
            if (nextChainDispatcher != null) {
                logger.debug("Unknown target name but have a chained dispatcher; passing on to next dispatcher");
                return nextChainDispatcher.internalHandleRequest(rawInput);
            } else {
                logger.error("Unknown target name and no chained dispatcher; cannot dispatch method");
                throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.", targetName);
            }
        }

        Object requestObject = parseRequestObject(definition, parsedInput);

        Object invokeResult;
        try {
            logger.trace("Invoking method...");
            invokeResult = definition.getTargetMethod().invoke(handlerInstance, requestObject);
        } catch (IllegalAccessException e) {
            // Shouldn't ever happen since methods are from interface and therefore always public
            throw new RuntimeException("Illegal handler method access", e);
        } catch (InvocationTargetException e) {
            // If exception is one our server exception types, unwrap it and throw directly.
            if (e.getCause() instanceof ApiServerException) {
                throw (ApiServerException) e.getCause();
            }
            // Otherwise, re-wrap in a RequestHandlingRuntimeException to mark it as an unexpected error.
            throw new RequestHandlingRuntimeException("Uncaught exception during request handling", e.getCause());
        }

        return serializeResponseObject(definition, invokeResult);

    }

}


