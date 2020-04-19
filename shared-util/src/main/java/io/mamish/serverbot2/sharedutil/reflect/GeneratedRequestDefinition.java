package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratedRequestDefinition {

    private final String name;
    private final String description;
    private final int order;
    private final int numRequiredFields;

    private final Method targetMethod;
    private final Class<?> requestDtoType;
    private final Constructor<?> requestDtoConstructor;

    private final List<Pair<Field,ApiArgumentInfo>> orderedFields;

    public GeneratedRequestDefinition(Method targetMethod) throws ReflectiveOperationException {

        Class<?> requestType = targetMethod.getParameterTypes()[0];
        ApiRequestInfo apiRequestInfo = requestType.getAnnotation(ApiRequestInfo.class);

        this.name = apiRequestInfo.name();
        this.description = apiRequestInfo.description();
        this.order = apiRequestInfo.order();
        this.numRequiredFields = apiRequestInfo.numRequiredFields();
        this.targetMethod = targetMethod;
        this.requestDtoType = requestType;
        this.requestDtoConstructor = requestType.getConstructor();

        orderedFields = Arrays.stream(requestType.getDeclaredFields())
                .map(f -> new Pair<>(f,f.getAnnotation(ApiArgumentInfo.class)))
                .sorted(Comparator.comparing(p -> p.snd().order()))
                .collect(Collectors.toList());
        orderedFields.forEach(f -> f.fst().setAccessible(true));

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

    public Class<?> getRequestDtoType() {
        return requestDtoType;
    }

    public Constructor<?> getRequestDtoConstructor() {
        return requestDtoConstructor;
    }

    public List<Pair<Field, ApiArgumentInfo>> getOrderedFields() {
        return orderedFields;
    }

    public List<Field> getOrderedFieldsFieldView() {
        return orderedFields.stream().map(Pair::fst).collect(Collectors.toUnmodifiableList());
    }

    public List<ApiArgumentInfo> getOrderedFieldsInfoView() {
        return orderedFields.stream().map(Pair::snd).collect(Collectors.toUnmodifiableList());
    }

}
