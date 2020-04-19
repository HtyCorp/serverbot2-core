package io.mamish.serverbot2.sharedutil.reflect;

import com.google.gson.*;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class JsonRequestDispatcher<HandlerType> extends
        AbstractRequestDispatcher<HandlerType,String,JsonObject,GeneratedRequestDefinition> {

    private Gson gson = new Gson();

    public JsonRequestDispatcher(HandlerType handler, Class<HandlerType> handlerInterfaceClass,
                                 Class<GeneratedRequestDefinition> definitionClass) {
        super(handler, handlerInterfaceClass, definitionClass);
    }

    @Override
    protected Pair<String, JsonObject> parseNameKey(String rawInput) throws UnparsableInputException {
        try {
            JsonObject obj = JsonParser.parseString(rawInput).getAsJsonObject();
            String targetName = obj.remove(CommonConfig.JSON_API_TARGET_KEY).getAsString();
            return new Pair<>(targetName, obj);
        } catch (RuntimeException e) {
            throw new UnparsableInputException("Could not parse request target name from JSON", e);
        }
    }

    @Override
    protected Object parseRequestObject(GeneratedRequestDefinition definition, JsonObject input) throws UnparsableInputException, RequestValidationException{
        Object parsedObject;
        try {
            parsedObject = gson.fromJson(input, definition.getClass());
        } catch (JsonSyntaxException e) {
            throw new UnparsableInputException("Invalid JSON request fields", e);
        }
        // Validate required JSON fields
        boolean allRequiredFieldsPresent = definition.getOrderedFieldsFieldView().stream()
                .limit(definition.getNumRequiredFields())
                .allMatch(field -> {
                    try {
                        return field.get(parsedObject) != null;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Unexpected access exception validation JSON field", e);
                    }
                });

        if (!allRequiredFieldsPresent) {
            throw new RequestValidationException("Required fields for request '" + definition.getName() + "' not provided.");
        }

        return parsedObject;
    }
}
