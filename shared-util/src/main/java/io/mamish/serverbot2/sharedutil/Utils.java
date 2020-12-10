package io.mamish.serverbot2.sharedutil;

import java.util.Arrays;
import java.util.List;
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

}
