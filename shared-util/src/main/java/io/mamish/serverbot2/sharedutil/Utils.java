package io.mamish.serverbot2.sharedutil;

import java.util.Arrays;

public class Utils {

    private Utils() {}

    public static boolean equalsAny(Object object, Object... options) {
        // Original version which IDEA suggested replacement for:
        // return Arrays.stream(options).anyMatch(object::equals);
        return Arrays.asList(options).contains(object);
    }

}
