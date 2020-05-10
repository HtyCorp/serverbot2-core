package io.mamish.serverbot2.framework.common;

public class ApiErrorInfo {

    private String exceptionTypeName;
    private String exceptionMessage;

    public ApiErrorInfo() {}

    public ApiErrorInfo(String exceptionTypeName, String exceptionMessage) {
        this.exceptionTypeName = exceptionTypeName;
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionTypeName() {
        return exceptionTypeName;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
