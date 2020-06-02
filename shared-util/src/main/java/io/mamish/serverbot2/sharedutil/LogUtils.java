package io.mamish.serverbot2.sharedutil;

import com.google.gson.Gson;

import java.util.function.Supplier;

public class LogUtils {

    private static final Gson gson = new Gson();

    private LogUtils() {}

    public static Supplier<String> dump(String msg, Object data) {
        return () -> msg + '\n' + gson.toJson(data);
    }

}
