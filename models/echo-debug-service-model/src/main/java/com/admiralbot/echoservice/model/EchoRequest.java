package com.admiralbot.echoservice.model;

import com.admiralbot.framework.modelling.ApiArgumentInfo;
import com.admiralbot.framework.modelling.ApiRequestInfo;

@ApiRequestInfo(name = "Echo", order = 0, numRequiredFields = 1,
        description = "Does exactly what you expect")
public class EchoRequest {

    @ApiArgumentInfo(order = 0, description = "The user's message to echo")
    private String message;

    @ApiArgumentInfo(order = 1, description = "Echo more aggressively")
    private boolean shout;

    public EchoRequest() {}

    public EchoRequest(String message, boolean shout) {
        this.message = message;
        this.shout = shout;
    }

    public String getMessage() {
        return message;
    }

    public boolean getShout() {
        return shout;
    }
}
