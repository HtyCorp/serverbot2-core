package com.admiralbot.madscientist.lambdaruntime;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class LambdaError {

    public static final String ERROR_TYPE_HEADER = "Lambda-Runtime-Function-Error-Type";

    private String errorMessage;
    private String errorType;
    private List<String> stackTrace;

}
