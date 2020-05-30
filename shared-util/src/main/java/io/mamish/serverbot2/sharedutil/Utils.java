package io.mamish.serverbot2.sharedutil;

import java.util.Arrays;

public class Utils {

    public static <T> boolean equalsAny(T object, T... options) {
        // Original version which IDEA suggested replacement for:
        // return Arrays.stream(options).anyMatch(object::equals);
        return Arrays.asList(options).contains(object);
    }

}
