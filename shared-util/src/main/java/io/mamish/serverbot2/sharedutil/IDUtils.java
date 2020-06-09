package io.mamish.serverbot2.sharedutil;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class IDUtils {

    private IDUtils() {}

    public static String slash(Object... segments) {
        return joinWith("/", segments);
    }

    public static String kebab(Object... segments) {
        return joinWith("-", segments);
    }

    public static String snake(Object... segments) {
        return joinWith("_", segments);
    }

    public static String colon(Object... segments) {
        return joinWith(":", segments);
    }

    public static String joinWith(String delim, Object... segments) {
        return Arrays.stream(segments).map(Object::toString).collect(Collectors.joining(delim));
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String randomUUIDJoined() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    public static String epochSeconds() {
        return Long.toString(Instant.now().getEpochSecond());
    }

    public static String epochMillis() {
        return Long.toString(Instant.now().toEpochMilli());
    }

}
