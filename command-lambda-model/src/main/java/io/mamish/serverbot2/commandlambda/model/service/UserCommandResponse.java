package io.mamish.serverbot2.commandlambda.model.service;

public class UserCommandResponse {

    private boolean sendInitialMessage;
    private String initialMessageContent;

    public UserCommandResponse(boolean sendInitialMessage, String initialMessageContent) {
        this.sendInitialMessage = sendInitialMessage;
        this.initialMessageContent = initialMessageContent;
    }

    public boolean shouldSendInitialMessage() {
        return sendInitialMessage;
    }

    public String getInitialMessageContent() {
        return initialMessageContent;
    }
}
