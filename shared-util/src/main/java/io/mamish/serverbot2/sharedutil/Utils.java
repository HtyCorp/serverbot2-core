package io.mamish.serverbot2.sharedutil;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Utils {

    public static boolean equalsAny(Object object, Object... options) {
        // Original version which IDEA suggested replacement for:
        // return Arrays.stream(options).anyMatch(object::equals);
        return Arrays.asList(options).contains(object);
    }

    public static void setGlobalLoggerLevel(Level level) {
        // Ref: https://stackoverflow.com/questions/6307648/change-global-setting-for-logger-instances
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(level);
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(level);
        }
    }

}
