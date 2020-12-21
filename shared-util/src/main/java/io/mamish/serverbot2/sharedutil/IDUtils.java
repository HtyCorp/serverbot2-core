package io.mamish.serverbot2.sharedutil;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class IDUtils {

    private IDUtils() {}

    public static String dot(Object... segments) {
        return joinWith(".", segments);
    }

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

    public static String randomIdShort() {
        int randomInt = ThreadLocalRandom.current().nextInt(0xffffff);
        return String.format("%06x", randomInt);
    }

    public static String epochSeconds() {
        return Long.toString(Instant.now().getEpochSecond());
    }

    public static String epochMillis() {
        return Long.toString(Instant.now().toEpochMilli());
    }

    public static String stripLeadingICharIfPresent(String className) {
        if (className.startsWith("I") && Character.isUpperCase(className.charAt(1))) {
            return className.substring(1);
        }
        return className;
    }

}
