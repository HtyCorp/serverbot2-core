package com.admiralbot.framework.common;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiDefinitionSet<ModelType> {

    private final ApiEndpointInfo endpointInfo;
    private final Map<String, ApiActionDefinition> nameToDefinitionMap;
    private final Map<Class<?>, ApiActionDefinition> requestClassToDefinitionMap;

    public ApiDefinitionSet(Class<ModelType> modelClass, boolean requireEndpointInfoPresent) {

        assert modelClass.isInterface();

        endpointInfo = modelClass.getAnnotation(ApiEndpointInfo.class);
        if (requireEndpointInfoPresent && endpointInfo == null) {
            throw new IllegalArgumentException("Service model interface is missing endpoint info annotation");
        }

        Stream<Method> allListenerInterfaceMethods;
        Function<Method, ApiActionDefinition> tryGenerateDefinition;
        Comparator<ApiActionDefinition> compareByOrderAttribute;
        BinaryOperator<ApiActionDefinition> errorOnNameCollision;
        Collector<ApiActionDefinition, ?, TreeMap<String, ApiActionDefinition>> collectToTreeMapWithNameAsKey;

        allListenerInterfaceMethods = Arrays.stream(modelClass.getDeclaredMethods());
        tryGenerateDefinition = method -> {
            try {
                return new ApiActionDefinition(method);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unexpected error while generating API definition", e);
            }
        };
        compareByOrderAttribute = Comparator.comparing(ApiActionDefinition::getOrder);
        errorOnNameCollision = (_a, _b) -> {
            throw new IllegalStateException("Command name collision while generating definitions: " + _a.getName());
        };

        // Using TreeMap to preserve insertion order by metadata order attribute.
        // Not strictly necessary now (as it is with ordered fields in request POJOs) but keeping just in case.
        collectToTreeMapWithNameAsKey = Collectors.toMap(ApiActionDefinition::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.nameToDefinitionMap = Collections.unmodifiableMap(allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByOrderAttribute)
                .collect(collectToTreeMapWithNameAsKey));

        this.requestClassToDefinitionMap = this.nameToDefinitionMap.values().stream().collect(Collectors.toUnmodifiableMap(
                ApiActionDefinition::getRequestDataType,
                Function.identity()
        ));
    }

    public ApiEndpointInfo getEndpointInfo() {
        return endpointInfo;
    }

    public ApiActionDefinition getFromName(String name) {
        return nameToDefinitionMap.get(name);
    }

    public ApiActionDefinition getFromRequestClass(Class<?> requestClass) {
        return requestClassToDefinitionMap.get(requestClass);
    }

    public Collection<ApiActionDefinition> getAll() {
        return nameToDefinitionMap.values();
    }

}
