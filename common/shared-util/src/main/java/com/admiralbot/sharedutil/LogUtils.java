package com.admiralbot.sharedutil;

import com.google.gson.Gson;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class LogUtils {

    private static final Gson gson = new Gson();

    private LogUtils() {}

    public static void info(Logger logger, Supplier<String> msgSupplier) {
        if (logger.isInfoEnabled()) {
            logger.info(msgSupplier.get());
        }
    }

    public static void debug(Logger logger, Supplier<String> msgSupplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(msgSupplier.get());
        }
    }

    public static void infoDump(Logger logger, String msg, Object data) {
        info(logger, () -> msg + "\n" + gson.toJson(data));
    }

    public static void debugDump(Logger logger, String msg, Object data) {
        debug(logger, () -> msg + "\n" + gson.toJson(data));
    }

}