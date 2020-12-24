package com.admiralbot.sharedutil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {}

    public static boolean equalsAny(Object object, Object... options) {
        // Original version which IDEA suggested replacement for:
        // return Arrays.stream(options).anyMatch(object::equals);
        return Arrays.asList(options).contains(object);
    }

    public static <U,V> List<V> mapList(List<U> inputList, Function<U,V> mapper) {
        return inputList.stream().map(mapper).collect(Collectors.toList());
    }

    public static boolean inRangeInclusive(long number, long min, long max) {
        return number >= min && number <= max;
    }

    public static <T,U> U mapNullable(T t, Function<T,U> mapper) {
        if (t != null) {
            return mapper.apply(t);
        }
        return null;
    }

    public static <T,U> void ifNotNull(T tValue, Function<T,U> uGetter, Consumer<U> action) {
        if (tValue != null) {
            U uValue = uGetter.apply(tValue);
            if (uValue != null) {
                action.accept(uValue);
            }
        }
    }

}
