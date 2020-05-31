package io.mamish.serverbot2.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectiveEquals {

    public static <T> boolean areEqual(T object1, T object2) {
        boolean null1 = object1 == null;
        boolean null2 = object2 == null;

        // If either or both are null, return result right now.
        if (null1 && null2) {
            return true;
        }
        if (null1 || null2) {
            return false;
        }

        // If these are both references to the same object, true.
        if (object1 == object2) {
            return true;
        }

        // Can have different classes since subclasses of T are allowed (unfortunately...)
        Class <?> c = object1.getClass();
        if (object2.getClass() != c) {
            return false;
        }

        // Previously checked for primitive but it turned out to be unnecessary:
        // field.get(...) boxes the type, and boxed types all appear to have a valid equals() method.

        // If an overridden equals() method exists, use its result directly.
        try {
            Class<?> equalsDeclarer = c.getMethod("equals", Object.class).getDeclaringClass();
            if (equalsDeclarer == c) {
                return object1.equals(object2);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Impossible: no equals method found on an object", e);
        }

        // All other cases exhausted: recursively compare fields to look for any mismatch.
        // Note this *does* check inherited fields: assuming this will be used on our own value types where extending
        // them will be rare but significant.
        Class<?> currentClass = c;
        while (currentClass != null) {
            try {
                for (Field f: currentClass.getDeclaredFields()) {
                    if (Modifier.isTransient(f.getModifiers())) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object val1 = f.get(object1);
                    Object val2 = f.get(object2);
                    if (!ReflectiveEquals.areEqual(val1, val2)) {
                        return false;
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Impossible: failed to reflectively get a field value", e);
            }
            currentClass = currentClass.getSuperclass();
        }

        return true;
    }

}
