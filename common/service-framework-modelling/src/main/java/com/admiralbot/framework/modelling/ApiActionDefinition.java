package com.admiralbot.framework.modelling;

import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.Pair;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Note: Making this as a replacement of "SimpleApiDefinition" to work with clients and servers.
 */
public class ApiActionDefinition {

    // Uses a default GSON instance since definitions should never be constructed at native-image runtime
    private static final Gson GSON = new Gson();

    private final String name;
    private final String description;
    private final int order;
    private final int numRequiredFields;

    private final Method targetMethod;

    private final Class<?> requestDataType;
    private final Constructor<?> requestTypeConstructor;
    private final TypeAdapter<?> requestTypeAdapter;

    private final boolean hasResponseType;
    private final Class<?> responseDataType;
    private final TypeAdapter<?> responseTypeAdapter;

    private final List<Pair<Field, ApiArgumentInfo>> orderedFields;
    private final List<Field> orderedFieldsFieldView;

    private final String usageString;
    private final List<String> argumentDescriptionStrings;

    public ApiActionDefinition(Method targetMethod) throws ReflectiveOperationException {

        Class<?> requestType = targetMethod.getParameterTypes()[0];
        ApiRequestInfo apiRequestInfo = requestType.getAnnotation(ApiRequestInfo.class);

        Class<?> responseType = targetMethod.getReturnType();

        this.name = apiRequestInfo.name();
        this.description = apiRequestInfo.description();
        this.order = apiRequestInfo.order();
        this.numRequiredFields = apiRequestInfo.numRequiredFields();
        this.targetMethod = targetMethod;
        this.requestDataType = requestType;
        this.requestTypeConstructor = requestType.getConstructor();
        this.requestTypeAdapter = GSON.getAdapter(requestDataType);

        if (responseType == Void.TYPE) {
            this.hasResponseType = false;
            this.responseDataType = null;
            this.responseTypeAdapter = null;
        } else {
            this.hasResponseType = true;
            this.responseDataType = responseType;
            this.responseTypeAdapter = GSON.getAdapter(responseDataType);
        }

        this.orderedFields = Arrays.stream(requestType.getDeclaredFields())
                .filter(f -> f.getAnnotation(ApiArgumentInfo.class) != null)
                .map(f -> new Pair<>(f,f.getAnnotation(ApiArgumentInfo.class)))
                .sorted(Comparator.comparing(p -> p.b().order()))
                .collect(Collectors.toUnmodifiableList());
        orderedFields.forEach(f -> f.a().setAccessible(true));
        this.orderedFieldsFieldView = orderedFields.stream().map(Pair::a).collect(Collectors.toUnmodifiableList());

        StringBuilder sbUsage = new StringBuilder();
        sbUsage.append(CommonConfig.COMMAND_SIGIL_CHARACTER).append(getName());
        for (int i = 0; i < orderedFieldsFieldView.size(); i++) {
            String argName = orderedFieldsFieldView.get(i).getName();
            sbUsage.append(' ');
            if (i < getNumRequiredFields()) {
                sbUsage.append(argName);
            } else {
                sbUsage.append('[').append(argName).append(']');
            }
        }

        this.usageString = sbUsage.toString();
        this.argumentDescriptionStrings = orderedFields.stream()
                .map(m -> m.a().getName() + ": " + m.b().description())
                .collect(Collectors.toList());

    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public int getNumRequiredFields() {
        return numRequiredFields;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public Class<?> getRequestDataType() {
        return requestDataType;
    }

    public Constructor<?> getRequestTypeConstructor() {
        return requestTypeConstructor;
    }

    public TypeAdapter<?> getRequestTypeAdapter() {
        return requestTypeAdapter;
    }

    public boolean hasResponseType() {
        return hasResponseType;
    }

    public Class<?> getResponseDataType() {
        return responseDataType;
    }

    public TypeAdapter<?> getResponseTypeAdapter() {
        return responseTypeAdapter;
    }

    public List<Pair<Field, ApiArgumentInfo>> getOrderedFields() {
        return orderedFields;
    }

    public List<Field> getOrderedFieldsFieldView() {
        return orderedFieldsFieldView;
    }

    public String getUsageString() {
        return usageString;
    }

    public List<String> getArgumentDescriptionStrings() {
        return argumentDescriptionStrings;
    }
}
