package com.admiralbot.framework.exception;

import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

@RegisterGsonType
public class ServerExceptionDto {

    private String exceptionTypeName;
    private String exceptionMessage;

    public ServerExceptionDto() {}

    public ServerExceptionDto(String exceptionTypeName, String exceptionMessage) {
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
