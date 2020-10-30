package io.mamish.serverbot2.framework.exception;

import io.mamish.serverbot2.framework.exception.server.*;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerExceptionParser {

    private static final List<Class<? extends ApiServerException>> exceptionClassList = List.of(
            FrameworkInternalException.class,
            NoSuchResourceException.class,
            RequestHandlingException.class,
            RequestHandlingRuntimeException.class,
            RequestValidationException.class,
            ResourceAlreadyExistsException.class,
            ResourceExpiredException.class,
            SerializationException.class,
            ServiceLimitException.class,
            UnknownRequestException.class,
            UnparsableInputException.class
    );
    private static final Map<String, Function<String,? extends ApiServerException>> generatorMap = exceptionClassList.stream()
        .collect(Collectors.toMap(Class::getSimpleName, ServerExceptionParser::makeGenerator));

    public static ApiServerException fromName(String typeSimpleName, String message) {
        var generator = generatorMap.get(typeSimpleName);
        if (generator == null) {
            return new ApiServerException(message);
        } else {
            return generator.apply(message);
        }
    }

    private static <E extends ApiServerException> Function<String,E> makeGenerator(Class<E> exceptionClass) {
        try {
            Constructor<E> constructor = exceptionClass.getConstructor(String.class);
            return (String message) -> {
                ApiServerException exception;
                try {
                    exception = constructor.newInstance(message);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Impossible: Exception extending ApiServerException with unusable message constructor", e);
                }
                throw exception;
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Impossible: Exception extending ApiServerException without message constructor", e);
        }
    }

}
