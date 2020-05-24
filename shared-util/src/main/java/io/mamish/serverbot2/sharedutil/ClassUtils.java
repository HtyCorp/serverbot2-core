package io.mamish.serverbot2.sharedutil;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ClassUtils {

    private ClassUtils() {}

    public static boolean instanceOfAny(Object object, Class<?>... classes) {
        return Arrays.stream(classes).anyMatch(c -> c.isInstance(object));
    }

    public static Field field(Class<?> clazz, String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
