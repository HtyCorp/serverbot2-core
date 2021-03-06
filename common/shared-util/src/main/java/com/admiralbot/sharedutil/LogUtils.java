package com.admiralbot.sharedutil;

import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;

public class LogUtils {

    private static final Gson gson = new Gson();

    private LogUtils() {}

    public static void infoDump(Logger logger, String msg, Object data) {
        logger.info(() -> msg + "\n" + gson.toJson(data));
    }

    public static void debugDump(Logger logger, String msg, Object data) {
        logger.debug(() -> msg + "\n" + gson.toJson(data));
    }

}
