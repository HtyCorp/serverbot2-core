package io.mamish.serverbot2.sharedutil;

import java.util.Arrays;

public class ClassUtils {

    private ClassUtils() {}

    public static boolean instanceOfAny(Object object, Class<?>... classes) {
        return Arrays.stream(classes).anyMatch(c -> c.isInstance(object));
    }

}
