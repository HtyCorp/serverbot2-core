package io.mamish.serverbot2.framework.server;

import io.mamish.serverbot2.framework.common.*;
import io.mamish.serverbot2.framework.exception.*;
import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractApiHandler<ModelType, OutputType, RawInputType, ParsedInputType> {

    private ModelType handlerInstance;
    private ApiDefinitionSet<?> apiDefinitionSet;

    private Logger logger = Logger.getLogger("AbstractApiHandler");

    public AbstractApiHandler(ModelType handlerInstance, Class<ModelType> handlerInterfaceClass) {
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

    protected abstract OutputType serializeErrorObject(ApiException exception);

    public final OutputType handleRequest(RawInputType rawInput) {
        try {
            return internalHandleRequest(rawInput);
        } catch (ApiException e) {
            logger.log(Level.WARNING, "ApiException in API request dispatcher", e);
            return serializeErrorObject(e);
        } catch (Exception e) {
            String message = "Unknown exception in API request dispatcher: " + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            return serializeErrorObject(new FrameworkInternalException(message));
        }
    }

    private OutputType internalHandleRequest(RawInputType rawInput) throws ApiException {

        Pair<String, ParsedInputType> nameAndRemainingInput = parseNameKey(rawInput);

        String targetName = nameAndRemainingInput.fst();
        ParsedInputType parsedInput = nameAndRemainingInput.snd();
        ApiActionDefinition definition = apiDefinitionSet.getFromName(targetName);

        if (definition == null) {
            throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.");
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


