package io.mamish.serverbot2.framework.server;

import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.common.ApiDefinitionSet;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.server.*;
import io.mamish.serverbot2.sharedutil.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractApiRequestDispatcher<ModelType, OutputType, RawInputType, ParsedInputType> {

    private final ModelType handlerInstance;
    private final ApiDefinitionSet<?> apiDefinitionSet;

    private final Logger logger = LogManager.getLogger(AbstractApiRequestDispatcher.class);

    private AbstractApiRequestDispatcher<?, OutputType, RawInputType, ?> nextChainDispatcher;

    public AbstractApiRequestDispatcher(ModelType handlerInstance, Class<ModelType> handlerInterfaceClass) {
        this.handlerInstance = handlerInstance;
        logger.trace("Building definition set for " + handlerInterfaceClass.getSimpleName());
        this.apiDefinitionSet = new ApiDefinitionSet<>(handlerInterfaceClass);
        logger.trace("Finished construction");
    }

    public ApiDefinitionSet<?> getApiDefinitionSet() {
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
            logger.error("ApiException in API request dispatcher", e);
            return serializeErrorObject(e);
        } catch (Exception e) {
            String message = "Unknown exception in API request dispatcher: " + e.getMessage();
            logger.error(message, e);
            return serializeErrorObject(new FrameworkInternalException(message));
        }
    }

    private OutputType internalHandleRequest(RawInputType rawInput) throws ApiException {

        Pair<String, ParsedInputType> nameAndRemainingInput = parseNameKey(rawInput);
        logger.debug("Parsed name key as: " + nameAndRemainingInput.a());

        String targetName = nameAndRemainingInput.a();
        ParsedInputType parsedInput = nameAndRemainingInput.b();
        ApiActionDefinition definition = apiDefinitionSet.getFromName(targetName);

        // If this dispatcher doesn't have any such request definition but is chained to another dispatch,
        // invoke that dispatcher to see if it can get a result.
        if (definition == null) {
            if (nextChainDispatcher != null) {
                logger.info("Unknown target name but have a chained dispatcher; passing on to next dispatcher");
                return nextChainDispatcher.internalHandleRequest(rawInput);
            } else {
                logger.warn("Unknown target name and no chained dispatcher; cannot dispatch method");
                throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.", targetName);
            }
        }

        logger.debug("Got a definition set");

        Object requestObject = parseRequestObject(definition, parsedInput);

        logger.debug("Definition is: " + definition.toString());
        logger.debug("Target method is: " + definition.getTargetMethod());
        logger.debug("Handler instance is: " + handlerInstance);
        logger.debug("Request object is: " + requestObject);

        Object invokeResult;
        try {
            logger.debug("Invoking method...");
            invokeResult = definition.getTargetMethod().invoke(handlerInstance, requestObject);
        } catch (IllegalAccessException e) {
            // Shouldn't ever happen since methods are from interface and therefore always public
            throw new RuntimeException("Illegal handler method access", e);
        } catch (InvocationTargetException e) {
            // If exception is a specifically thrown RequestHandlingException, unwrap it and throw directly.
            if (e.getCause() instanceof RequestHandlingException) {
                throw (RequestHandlingException) e.getCause();
            }
            // Otherwise, re-wrap in a RequestHandlingRuntimeException to mark it as an unexpected error.
            throw new RequestHandlingRuntimeException("Uncaught exception during request handling", e.getCause());
        }

        return serializeResponseObject(definition, invokeResult);

    }

}


