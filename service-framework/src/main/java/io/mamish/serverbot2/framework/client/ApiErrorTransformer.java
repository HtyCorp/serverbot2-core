package io.mamish.serverbot2.framework.client;

import io.mamish.serverbot2.framework.exception.*;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiErrorTransformer {

    private static List<Class<? extends ApiException>> exceptionClassList = List.of(
            FrameworkInternalException.class,
            RequestHandlingException.class,
            RequestHandlingRuntimeException.class,
            SerializationException.class,
            UnknownRequestException.class,
            UnparsableInputException.class
    );
    private static Map<String, Function<String,? extends ApiException>> generatorMap = exceptionClassList.stream()
        .collect(Collectors.toMap(Class::getSimpleName, ApiErrorTransformer::makeGenerator));

    public static ApiException fromName(String typeSimpleName, String message) {
        var generator = generatorMap.get(typeSimpleName);
        if (generator == null) {
            return new ApiException(message);
        } else {
            return generator.apply(message);
        }
    }

    private static <E extends ApiException> Function<String,E> makeGenerator(Class<E> exceptionClass) {
        try {
            Constructor<E> constructor = exceptionClass.getConstructor(String.class);
            return (String message) -> {
                ApiException exception;
                try {
                    exception = constructor.newInstance(message);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Impossible: Exception extending ApiException with unusable message constructor", e);
                }
                throw exception;
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Impossible: Exception extending ApiException without message constructor", e);
        }
    }

}
