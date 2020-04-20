package io.mamish.serverbot2.sharedutil.reflect;

import com.google.gson.*;
import io.mamish.serverbot2.sharedutil.AnnotatedGson;
import io.mamish.serverbot2.sharedutil.Pair;

public class JsonRequestDispatcher<HandlerType> extends
        AbstractRequestDispatcher<HandlerType,String,JsonObject,String,SimpleApiDefinition> {

    private AnnotatedGson annotatedGson = new AnnotatedGson();

    public JsonRequestDispatcher(HandlerType handler, Class<HandlerType> handlerInterfaceClass) {
        super(handler, handlerInterfaceClass, SimpleApiDefinition::new);
    }

    @Override
    protected Pair<String, JsonObject> parseNameKey(String rawInput) {
        return annotatedGson.fromJsonWithTargetName(rawInput);
    }

    @Override
    protected Object parseRequestObject(SimpleApiDefinition definition, JsonObject input) {
        Object parsedObject;
        try {
            parsedObject = annotatedGson.getGson().fromJson(input, definition.getRequestDtoType());
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
    protected String serializeResponseObject(SimpleApiDefinition definition, Object handlerResult) {
        return annotatedGson.toJson(handlerResult);
    }
}
