package com.admiralbot.sharedutil;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Joiner {
    public static String dot(Object... segments) {
        return with(".", segments);
    }

    public static String slash(Object... segments) {
        return with("/", segments);
    }

    public static String kebab(Object... segments) {
        return with("-", segments);
    }

    public static String snake(Object... segments) {
        return with("_", segments);
    }

    public static String colon(Object... segments) {
        return with(":", segments);
    }

    public static String space(Object... segments) {
        return with(" ", segments);
    }

    public static String with(String delim, Object... segments) {
        return Arrays.stream(segments).map(Object::toString).collect(Collectors.joining(delim));
    }
}
