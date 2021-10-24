package com.admiralbot.framework.server;

import com.admiralbot.framework.exception.ServerExceptionDto;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.SerializationException;
import com.admiralbot.framework.exception.server.UnparsableInputException;
import com.admiralbot.framework.modelling.ApiActionDefinition;
import com.admiralbot.nativeimagesupport.cache.ImageCache;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedutil.Pair;
import com.google.gson.*;

public class JsonApiRequestDispatcher<HandlerType> extends
        AbstractApiRequestDispatcher<HandlerType,String,String,JsonObject> {

    private final static Gson GSON = ImageCache.getGson();

    public JsonApiRequestDispatcher(HandlerType handler, Class<HandlerType> handlerInterfaceClass,
                                    boolean requiresEndpointInfo) {
        super(handler, handlerInterfaceClass, requiresEndpointInfo);
    }

    @Override
    protected Pair<String, JsonObject> parseNameKey(String jsonString) {
        try {
            JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
            String targetName = obj.remove(ApiConfig.JSON_REQUEST_TARGET_KEY).getAsString();
            // Just remove this for now. Will make accessible to request handler later if required.
            obj.remove(ApiConfig.JSON_REQUEST_ID_KEY);
            return new Pair<>(targetName, obj);
        } catch (RuntimeException e) {
            throw new UnparsableInputException("Could not parse request target name from JSON", e);
        }
    }

    @Override
    protected Object parseRequestObject(ApiActionDefinition definition, JsonObject input) {
        Object parsedObject;
        try {
            parsedObject = definition.getRequestTypeAdapter().fromJsonTree(input);
        } catch (JsonSyntaxException e) {
            throw new UnparsableInputException("Invalid JSON request fields", e);
        }
        // Validate required JSON fields
        boolean allRequiredFieldsPresent = definition.getOrderedFieldsFieldView().stream()
                .limit(definition.getNumRequiredFields())
                .allMatch(field -> {
                    try {
                        Object requiredFieldValue = field.get(parsedObject);
                        return requiredFieldValue != null;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Unexpected access exception validation JSON field", e);
                    }
                });

        if (!allRequiredFieldsPresent) {
            throw new RequestValidationException("Required fields for request '" + definition.getName() + "' not provided.");
        }

        return parsedObject;
    }

    @Override
    protected String serializeResponseObject(ApiActionDefinition definition, Object handlerResult) {
        JsonObject resultObject = null;
        if (handlerResult != null) {
            if (!(definition.getResponseDataType().isInstance(handlerResult))) {
                throw new SerializationException("Response has unexpected type: " + handlerResult.getClass());
            }
            resultObject = responseToJsonObject(handlerResult, definition);
        }

        JsonObject finalObject = new JsonObject();
        finalObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, resultObject);
        finalObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, null);

        return GSON.toJson(finalObject);
    }

    @SuppressWarnings("unchecked")
    private <T> JsonObject responseToJsonObject(T response, ApiActionDefinition definition) {
        TypeAdapter<T> adapter = (TypeAdapter<T>) definition.getResponseTypeAdapter();
        return adapter.toJsonTree(response).getAsJsonObject();
    }

    @Override
    protected String serializeErrorObject(ApiServerException exception) {
        ServerExceptionDto info = new ServerExceptionDto(exception.getClass().getSimpleName(), exception.getMessage());
        JsonObject infoObject = GSON.toJsonTree(info).getAsJsonObject();

        JsonObject finalObject = new JsonObject();
        finalObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, infoObject);
        finalObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, null);

        return GSON.toJson(finalObject);
    }
}
