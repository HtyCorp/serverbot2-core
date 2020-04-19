package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractRequestDispatcher<HandlerType, ParseInputType, ProcessedInputType, OutputType,
        DefinitionType extends SimpleApiDefinition> {

    private HandlerType handlerInstance;
    private Map<String, DefinitionType> definitionMap;

    public AbstractRequestDispatcher(HandlerType handlerInstance, Class<HandlerType> handlerInterfaceClass,
                                     Class<DefinitionType> definitionClass) {

        assert handlerInterfaceClass.isInterface();
        this.handlerInstance = handlerInstance;

        Constructor<DefinitionType> definitionConstructor;

        Stream<Method> allListenerInterfaceMethods;
        Function<Method, DefinitionType> tryGenerateDefinition;
        Comparator<DefinitionType> compareByOrderAttribute;
        BinaryOperator<DefinitionType> errorOnNameCollision;
        Collector<DefinitionType, ?, TreeMap<String,DefinitionType>> collectToTreeMapWithNameAsKey;

        try {
            definitionConstructor = definitionClass.getConstructor(Method.class);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalStateException("Failed to generate definition constructor", nsme);
        }
        allListenerInterfaceMethods = Arrays.stream(handlerInterfaceClass.getDeclaredMethods());
        tryGenerateDefinition = method -> {
            try {
                return definitionConstructor.newInstance(method);
            } catch (ReflectiveOperationException roe) {
                throw new IllegalStateException("Invalid command definitions", roe);
            }
        };
        compareByOrderAttribute = Comparator.comparing(DefinitionType::getOrder);
        errorOnNameCollision = (_a, _b) -> {
            throw new IllegalStateException("Command name collision while generating definitions: " + _a.getName());
        };

        // Using TreeMap to preserve insertion order by metadata order attribute.
        // Not strictly necessary now (as it is with ordered fields in request POJOs but keeping just in case.
        collectToTreeMapWithNameAsKey = Collectors.toMap(DefinitionType::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.definitionMap = Collections.unmodifiableMap(allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByOrderAttribute)
                .collect(collectToTreeMapWithNameAsKey));

    }

    public Map<String,DefinitionType> getDefinitionMap() {
        return definitionMap;
    }

    public OutputType dispatch(ParseInputType rawInput)
            throws UnknownRequestException, UnparsableInputException, RequestValidationException, InvocationTargetException {

        Pair<String, ProcessedInputType> nameAndRemainingInput = parseNameKey(rawInput);

        String targetName = nameAndRemainingInput.fst();
        ProcessedInputType processedInput = nameAndRemainingInput.snd();
        DefinitionType definition = definitionMap.get(targetName);

        if (definition == null) {
            throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.");
        }
        Object requestObject = parseRequestObject(definition, processedInput);

        Object invokeResult;
        try {
            invokeResult = definition.getTargetMethod().invoke(handlerInstance, requestObject);
        } catch (IllegalAccessException e) {
            // Shouldn't ever happen since methods are from interface
            throw new RuntimeException("Illegal handler method access", e);
        }

        return serializeResponseObject(definition, invokeResult);

    }

    protected abstract Pair<String, ProcessedInputType> parseNameKey(ParseInputType input)
            throws UnparsableInputException;

    protected abstract Object parseRequestObject(DefinitionType definition, ProcessedInputType processedInput)
            throws UnparsableInputException, RequestValidationException;

    protected abstract OutputType serializeResponseObject(DefinitionType definition, Object handlerResult);

}
