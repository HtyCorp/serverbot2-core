package io.mamish.serverbot2.sharedutil;

import java.time.Instant;
import java.util.UUID;

public class IDUtils {

    private IDUtils() {}

    public static String slash(String... segment) {
        return String.join("/", segment);
    }

    public static String kebab(String... segments) {
        return String.join("-", segments);
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
