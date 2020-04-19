package io.mamish.serverbot2.commandlambda.model.service;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Objects;

public class CommandServiceResponse {

    private String optionalMessageContent;
    private String optionalMessageExternalId;

    public CommandServiceResponse(String optionalMessageContent) {
        this.optionalMessageContent = optionalMessageContent;
    }

    public CommandServiceResponse(String optionalMessageContent, String optionalMessageExternalId) {
        this.optionalMessageContent = optionalMessageContent;
        this.optionalMessageExternalId = optionalMessageExternalId;
    }

    public String getOptionalMessageContent() {
        return optionalMessageContent;
    }

    public String getOptionalMessageExternalId() {
        return optionalMessageExternalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandServiceResponse that = (CommandServiceResponse) o;
        return optionalMessageContent.equals(that.optionalMessageContent) &&
                Objects.equals(optionalMessageExternalId, that.optionalMessageExternalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionalMessageContent, optionalMessageExternalId);
    }

    @Override
    public String toString() {
        return "UserCommandResponse{" +
                "optionalMessageContent='" + StringEscapeUtils.escapeJava(optionalMessageContent) + '\'' +
                ", optionalMessageExternalId='" + StringEscapeUtils.escapeJava(optionalMessageExternalId) + '\'' +
                '}';
    }
}
