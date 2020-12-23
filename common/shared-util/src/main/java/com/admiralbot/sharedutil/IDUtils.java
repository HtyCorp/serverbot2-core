package com.admiralbot.sharedutil;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class IDUtils {

    private IDUtils() {}

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
