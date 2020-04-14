package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.MessageChannel;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LambdaHandlerTest {

    private static final String DUMMY_USER_ID = "12345678901234567";

    @Test
    public void testMissingCommand() {

        LambdaHandler handler = new LambdaHandler();

        UserCommandRequest request = new UserCommandRequest(
                List.of("notarealcommand", "arg0", "arg1"),
                MessageChannel.STANDARD,
                DUMMY_USER_ID
        );

        UserCommandResponse expectedResponse = new UserCommandResponse(
                "Error: !notarealcommand is not a recognised command.",
                null
        );

        UserCommandResponse actualResponse = handler.handleRequest(request, null);

        assert actualResponse.equals(expectedResponse);
    }

    @Test
    public void testStartMissingArgument() {

        LambdaHandler handler = new LambdaHandler();

        UserCommandRequest request = new UserCommandRequest(
                List.of("start"),
                MessageChannel.STANDARD,
                DUMMY_USER_ID
        );

        String expectedResponseString = "Error: expected at least 1 argument but got 0."
                + "\nUsage: !start game-name"
                + "\nUse '!help start' for details.";
        UserCommandResponse expectedResponse = new UserCommandResponse(expectedResponseString, null);

        UserCommandResponse actualResponse = handler.handleRequest(request, null);

        Assertions.assertEquals(expectedResponse, actualResponse);

    }

}
