package io.mamish.serverbot2.sharedutil;

import java.util.concurrent.Callable;

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

}
