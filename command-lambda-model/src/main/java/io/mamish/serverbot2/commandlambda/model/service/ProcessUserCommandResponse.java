package io.mamish.serverbot2.commandlambda.model.service;

import java.util.Objects;

public class ProcessUserCommandResponse {

    private String optionalMessageContent;
    private String optionalMessageExternalId;

    @SuppressWarnings("unused")
    public ProcessUserCommandResponse() { }

    public ProcessUserCommandResponse(String optionalMessageContent) {
        this.optionalMessageContent = optionalMessageContent;
    }

    public ProcessUserCommandResponse(String optionalMessageContent, String optionalMessageExternalId) {
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
        ProcessUserCommandResponse that = (ProcessUserCommandResponse) o;
        return Objects.equals(optionalMessageContent, that.optionalMessageContent) &&
                Objects.equals(optionalMessageExternalId, that.optionalMessageExternalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionalMessageContent, optionalMessageExternalId);
    }

    @Override
    public String toString() {
        return "CommandServiceResponse{" +
                "optionalMessageContent='" + optionalMessageContent + '\'' +
                ", optionalMessageExternalId='" + optionalMessageExternalId + '\'' +
                '}';
    }
}
