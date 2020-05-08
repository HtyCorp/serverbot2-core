package io.mamish.serverbot2.framework.common;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiDefinitionSet<ModelType> {

    private final Map<String, BasicApiDefinition> nameToDefinitionMap;
    private final Map<Class<?>, BasicApiDefinition> requestClassToDefinitionMap;

    public ApiDefinitionSet(Class<ModelType> modelClass) {

        assert modelClass.isInterface();

        Stream<Method> allListenerInterfaceMethods;
        Function<Method, BasicApiDefinition> tryGenerateDefinition;
        Comparator<BasicApiDefinition> compareByOrderAttribute;
        BinaryOperator<BasicApiDefinition> errorOnNameCollision;
        Collector<BasicApiDefinition, ?, TreeMap<String,BasicApiDefinition>> collectToTreeMapWithNameAsKey;

        allListenerInterfaceMethods = Arrays.stream(modelClass.getDeclaredMethods());
        tryGenerateDefinition = method -> {
            try {
                return new BasicApiDefinition(method);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unexpected error while generating API definition");
            }
        };
        compareByOrderAttribute = Comparator.comparing(BasicApiDefinition::getOrder);
        errorOnNameCollision = (_a, _b) -> {
            throw new IllegalStateException("Command name collision while generating definitions: " + _a.getName());
        };

        // Using TreeMap to preserve insertion order by metadata order attribute.
        // Not strictly necessary now (as it is with ordered fields in request POJOs) but keeping just in case.
        collectToTreeMapWithNameAsKey = Collectors.toMap(BasicApiDefinition::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.nameToDefinitionMap = Collections.unmodifiableMap(allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByOrderAttribute)
                .collect(collectToTreeMapWithNameAsKey));

        this.requestClassToDefinitionMap = this.nameToDefinitionMap.values().stream().collect(Collectors.toUnmodifiableMap(
                BasicApiDefinition::getRequestDataType,
                Function.identity()
        ));
    }

    public BasicApiDefinition getFromName(String name) {
        return nameToDefinitionMap.get(name);
    }

    public BasicApiDefinition getFromRequestClass(Class<?> requestClass) {
        return requestClassToDefinitionMap.get(requestClass);
    }

    public Collection<BasicApiDefinition> getAll() {
        return nameToDefinitionMap.values();
    }

}
