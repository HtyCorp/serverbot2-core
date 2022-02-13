package com.admiralbot.lambdaruntime;

import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

import java.util.List;

@RegisterGsonType
public class LambdaError {

    public static final String ERROR_TYPE_HEADER = "Lambda-Runtime-Function-Error-Type";

    private final String errorMessage;
    private final String errorType;
    private final List<String> stackTrace;

    // No default constructor necessary: used for serialization only

    public LambdaError(String errorMessage, String errorType, List<String> stackTrace) {
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.stackTrace = stackTrace;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }
}