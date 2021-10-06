package com.admiralbot.framework.server;

import com.admiralbot.framework.exception.ServerExceptionDto;
import com.admiralbot.framework.exception.server.ApiServerException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.UnparsableInputException;
import com.admiralbot.framework.modelling.ApiActionDefinition;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedutil.Pair;
import com.google.gson.*;

public class JsonApiRequestDispatcher<HandlerType> extends
        AbstractApiRequestDispatcher<HandlerType,String,String,JsonObject> {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

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
            parsedObject = gson.fromJson(input, definition.getRequestDataType());
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
            resultObject = gson.toJsonTree(handlerResult).getAsJsonObject();
        }

        JsonObject finalObject = new JsonObject();
        finalObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, resultObject);
        finalObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, null);

        return gson.toJson(finalObject);
    }

    @Override
    protected String serializeErrorObject(ApiServerException exception) {
        ServerExceptionDto info = new ServerExceptionDto(exception.getClass().getSimpleName(), exception.getMessage());
        JsonObject infoObject = gson.toJsonTree(info).getAsJsonObject();

        JsonObject finalObject = new JsonObject();
        finalObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, infoObject);
        finalObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, null);

        return gson.toJson(finalObject);
    }
}
