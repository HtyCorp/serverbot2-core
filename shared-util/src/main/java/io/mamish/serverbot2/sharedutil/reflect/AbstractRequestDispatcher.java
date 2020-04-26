package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractRequestDispatcher<HandlerType, ParseInputType, ProcessedInputType, OutputType,
        DefinitionType extends SimpleApiDefinition> {

    private HandlerType handlerInstance;
    private Map<String, DefinitionType> definitionMap;

    protected abstract Pair<String, ProcessedInputType> parseNameKey(ParseInputType input)
            throws UnparsableInputException;

    // return type unspecified since its unique to each request definition
    protected abstract Object parseRequestObject(DefinitionType definition, ProcessedInputType processedInput)
            throws UnparsableInputException, RequestValidationException;

    protected abstract OutputType serializeResponseObject(DefinitionType definition, Object handlerResult);

    public AbstractRequestDispatcher(HandlerType handlerInstance, Class<HandlerType> handlerInterfaceClass,
                                     ReflectFunction<Method,DefinitionType> definitionGenerator) {

        assert handlerInterfaceClass.isInterface();
        this.handlerInstance = handlerInstance;

        Stream<Method> allListenerInterfaceMethods;
        Function<Method, DefinitionType> tryGenerateDefinition;
        Comparator<DefinitionType> compareByOrderAttribute;
        BinaryOperator<DefinitionType> errorOnNameCollision;
        Collector<DefinitionType, ?, TreeMap<String,DefinitionType>> collectToTreeMapWithNameAsKey;

        allListenerInterfaceMethods = Arrays.stream(handlerInterfaceClass.getDeclaredMethods());
        tryGenerateDefinition = method -> {
            try {
                return definitionGenerator.apply(method);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unexpected error while generating API definition");
            }
        };
        compareByOrderAttribute = Comparator.comparing(DefinitionType::getOrder);
        errorOnNameCollision = (_a, _b) -> {
            throw new IllegalStateException("Command name collision while generating definitions: " + _a.getName());
        };

        // Using TreeMap to preserve insertion order by metadata order attribute.
        // Not strictly necessary now (as it is with ordered fields in request POJOs) but keeping just in case.
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
            throws UnknownRequestException, UnparsableInputException, RequestValidationException,
            RequestHandlingException, RequestHandlingRuntimeException {

        Pair<String, ProcessedInputType> nameAndRemainingInput = parseNameKey(rawInput);

        String targetName = nameAndRemainingInput.fst();
        ProcessedInputType processedInput = nameAndRemainingInput.snd();
        DefinitionType definition = definitionMap.get(targetName);

        if (definition == null) {
            throw new UnknownRequestException(targetName, "Unknown API target '" + targetName + "' in request.");
        }
        Object requestObject = parseRequestObject(definition, processedInput);

        Object invokeResult;
        try {
            invokeResult = definition.getTargetMethod().invoke(handlerInstance, requestObject);
        } catch (IllegalAccessException e) {
            // Shouldn't ever happen since methods are from interface and therefore always public
            throw new RuntimeException("Illegal handler method access", e);
        } catch (InvocationTargetException e) {
            // If exception is a specifically thrown RequestHandlingException, unwrap it and throw directly.
            if (e.getCause() instanceof RequestHandlingException) {
                throw (RequestHandlingException) e.getCause();
            }
            // Otherwise, re-wrap in a RequestHandlingRuntimeException to mark it as an unexpected error.
            throw new RequestHandlingRuntimeException("Uncaught exception during request handling", e.getCause());
        }

        return serializeResponseObject(definition, invokeResult);

    }

}
