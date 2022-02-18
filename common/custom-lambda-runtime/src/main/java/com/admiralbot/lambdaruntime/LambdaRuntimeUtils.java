package com.admiralbot.lambdaruntime;

import com.admiralbot.sharedutil.Utils;

public class LambdaRuntimeUtils {

    // https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html#runtimes-custom-build
    private static final String HANDLER_ENV_VAR_KEY = "_HANDLER";

    private LambdaRuntimeUtils() {}

    public static String getHandler() {
        return Utils.getEnvOrThrow(HANDLER_ENV_VAR_KEY);
    }

}
