package io.mamish.serverbot2.framework.server;

import com.google.gson.*;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.ServerExceptionDto;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.framework.exception.server.UnparsableInputException;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedutil.Pair;

public class JsonApiRequestDispatcher<HandlerType> extends
        AbstractApiRequestDispatcher<HandlerType,String,String,JsonObject> {

    private Gson gson = new GsonBuilder().serializeNulls().create();

    public JsonApiRequestDispatcher(HandlerType handler, Class<HandlerType> handlerInterfaceClass) {
        super(handler, handlerInterfaceClass);
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
        if (handlerResult == null) {
            return null;
        }

        JsonObject resultObject = gson.toJsonTree(handlerResult).getAsJsonObject();
        JsonObject finalObject = new JsonObject();

        resultObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, resultObject);
        resultObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, null);

        return gson.toJson(finalObject);
    }

    @Override
    protected String serializeErrorObject(ApiException exception) {
        ServerExceptionDto info = new ServerExceptionDto(exception.getClass().getSimpleName(), exception.getMessage());
        JsonObject infoObject = gson.toJsonTree(info).getAsJsonObject();

        JsonObject finalObject = new JsonObject();
        finalObject.add(ApiConfig.JSON_RESPONSE_ERROR_KEY, infoObject);
        finalObject.add(ApiConfig.JSON_RESPONSE_CONTENT_KEY, null);

        return gson.toJson(finalObject);
    }
}
