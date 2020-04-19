package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractRequestDispatcher<HandlerType, ParseInputType, ProcessedInputType,
        DefinitionType extends GeneratedRequestDefinition> {

    private HandlerType handler;
    private TreeMap<String, DefinitionType> definitionMap;

    public AbstractRequestDispatcher(HandlerType handler, Class<HandlerType> handlerInterfaceClass,
                                     Class<DefinitionType> definitionClass) {

        assert handlerInterfaceClass.isInterface();
        this.handler = handler;

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
        collectToTreeMapWithNameAsKey = Collectors.toMap(DefinitionType::getName,
                Function.identity(),
                errorOnNameCollision,
                TreeMap::new);

        this.definitionMap = allListenerInterfaceMethods
                .map(tryGenerateDefinition)
                .sorted(compareByOrderAttribute)
                .collect(collectToTreeMapWithNameAsKey);

    }

    public void dispatch(HandlerType handler, ParseInputType rawInput)
            throws UnknownRequestException, UnparsableInputException, RequestValidationException, InvocationTargetException {

        Pair<String, ProcessedInputType> nameAndRemainingInput = parseNameKey(rawInput);

        String targetName = nameAndRemainingInput.fst();
        ProcessedInputType processedInput = nameAndRemainingInput.snd();
        DefinitionType definition = definitionMap.get(targetName);

        if (definition == null) {
            throw new UnknownRequestException("Unknown API target '" + targetName + "' in request.");
        }
        Object requestObject = parseRequestObject(definition, processedInput);

        try {
            definition.getTargetMethod().invoke(handler, requestObject);
        } catch (IllegalAccessException e) {
            // Shouldn't ever happen since methods are from interface
            throw new RuntimeException("Illegal handler method access", e);
        }

    }

    protected abstract Pair<String, ProcessedInputType> parseNameKey(ParseInputType input)
            throws UnparsableInputException;

    protected abstract Object parseRequestObject(DefinitionType definition, ProcessedInputType processedInput)
            throws UnparsableInputException, RequestValidationException;

}
