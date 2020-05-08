package io.mamish.serverbot2.framework.common;

import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.ApiRequestInfo;

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
public class BasicApiDefinition {

    private final String name;
    private final String description;
    private final int order;
    private final int numRequiredFields;

    private final Method targetMethod;

    private final Class<?> requestDataType;
    private final Constructor<?> requestTypeConstructor;

    private final boolean hasResponseType;
    private final Class<?> responseDataType;
    private final Constructor<?> responseTypeConstructor;

    private final List<Pair<Field, ApiArgumentInfo>> orderedFields;
    private final List<Field> orderedFieldsFieldView;
    private final List<ApiArgumentInfo> orderedFieldsInfoView;

    public BasicApiDefinition(Method targetMethod) throws ReflectiveOperationException {

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

        if (responseType == Void.TYPE) {
            this.hasResponseType = false;
            this.responseDataType = null;
            this.responseTypeConstructor = null;
        } else {
            this.hasResponseType = true;
            this.responseDataType = responseType;
            this.responseTypeConstructor = responseType.getConstructor();
        }

        this.orderedFields = Arrays.stream(requestType.getDeclaredFields())
                .filter(f -> f.getAnnotation(ApiArgumentInfo.class) != null)
                .map(f -> new Pair<>(f,f.getAnnotation(ApiArgumentInfo.class)))
                .sorted(Comparator.comparing(p -> p.snd().order()))
                .collect(Collectors.toUnmodifiableList());
        orderedFields.forEach(f -> f.fst().setAccessible(true));
        this.orderedFieldsFieldView = orderedFields.stream().map(Pair::fst).collect(Collectors.toUnmodifiableList());
        this.orderedFieldsInfoView = orderedFields.stream().map(Pair::snd).collect(Collectors.toUnmodifiableList());

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

    public boolean hasResponseType() {
        return hasResponseType;
    }

    public Class<?> getResponseDataType() {
        return responseDataType;
    }

    public Constructor<?> getResponseTypeConstructor() {
        return responseTypeConstructor;
    }

    public List<Pair<Field, ApiArgumentInfo>> getOrderedFields() {
        return orderedFields;
    }

    public List<Field> getOrderedFieldsFieldView() {
        return orderedFieldsFieldView;
    }

    public List<ApiArgumentInfo> getOrderedFieldsInfoView() {
        return orderedFieldsInfoView;
    }
}
