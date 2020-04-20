package io.mamish.serverbot2.sharedutil.reflect;

import java.util.function.Function;

@FunctionalInterface
public interface ReflectFunction<T,U> {
    U apply(T t) throws ReflectiveOperationException;
}
