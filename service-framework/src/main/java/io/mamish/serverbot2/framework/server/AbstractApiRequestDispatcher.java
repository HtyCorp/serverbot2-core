package io.mamish.serverbot2.framework.server;

import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.common.ApiDefinitionSet;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.server.*;
import io.mamish.serverbot2.sharedutil.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public abstract class AbstractApiRequestDispatcher<ModelType, OutputType, RawInputType, ParsedInputType> {

    private final ModelType handlerInstance;
    private final ApiDefinitionSet<?> apiDefinitionSet;

    private final Logger logger = LogManager.getLogger(AbstractApiRequestDispatcher.class);

    private AbstractApiRequestDispatcher<?, OutputType, RawInputType, ?> nextChainDispatcher;

    public AbstractApiRequestDispatcher(ModelType handlerInstance, Class<ModelType> handlerInterfaceClass) {
        this.handlerInstance = handlerInstance;
        this.apiDefinitionSet = new ApiDefinitionSet<>(handlerInterfaceClass);
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

        String targetName = nameAndRemainingInput.a();
        ParsedInputType parsedInput = nameAndRemainingInput.b();
        ApiActionDefinition definition = apiDefinitionSet.getFromName(targetName);

        // If this dispatcher doesn't have any such request definition but is chained to another dispatch,
        // invoke that dispatcher to see if it can get a result.
        if (definition == null) {
            if (nextChainDispatcher != null) {
                return nextChainDispatcher.internalHandleRequest(rawInput);
            } else {
                throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.", targetName);
            }
        }

        Object requestObject = parseRequestObject(definition, parsedInput);

        Object invokeResult;
        try {
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


