package com.admiralbot.sharedutil;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionUtils {

    private ExceptionUtils() {}

    // Utility method to automatically catch exceptions and throw RuntimeException.
    // Useful for methods that are guaranteed to work but still have inconvenient checked exceptions,
    // e.g. using MessageDigest.getInstance with a mandatory algorithm.
    public static <T> T cantFail(Callable<T> resultGetter) {
        try {
            return resultGetter.call();
        } catch (Exception e) {
            throw new RuntimeException("Impossible failure!", e);
        }
    }

    /**
     * Convenience method to get a value or provide a default if a specific exception or error type occurs.
     *
     * @param resultGetter  Getter for the normal result value
     * @param throwableType The type of throwable to catch and provide a default value for
     * @param defaultGetter Function to return the default value given a throwable instance
     * @param <V>           The result type
     * @param <E>           The throwable type
     * @return The normal result value or, if the expected throwable occurred, a generated default value
     */
    public static <V, E extends Throwable> V defaultOnThrow(Supplier<V> resultGetter, Class<E> throwableType,
                                                            Function<E,V> defaultGetter) {
        try {
            return resultGetter.get();
        } catch (Throwable t) {
            if (throwableType.isInstance(t)) {
                return defaultGetter.apply(throwableType.cast(t));
            }
            throw t;
        }
    }

}
