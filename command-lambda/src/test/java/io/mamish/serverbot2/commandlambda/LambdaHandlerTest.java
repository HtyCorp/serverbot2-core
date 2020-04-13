package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.MessageChannel;
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

}
