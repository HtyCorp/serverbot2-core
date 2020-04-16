package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.service.UserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.service.UserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class LambdaHandlerTest {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;
    private static final String DUMMY_USER_ID = "12345678901234567";

    private Logger logger = Logger.getLogger("LambdaHandlerTest");

    @Test
    public void testMissingCommand() {
        testSimpleResponseMessage("Error: "+SIGIL+"notarealcommand is not a recognised command.",
                "notarealcommand", "arg0", "arg1");
    }

    @Test
    public void testStartMissingArgument() {

        String expectedResponseMessage = "Error: expected at least 1 argument but got 0."
                + "\nUsage: "+SIGIL+"start game-name"
                + "\nUse '"+SIGIL+"help start' for details.";

        testSimpleResponseMessage(expectedResponseMessage,
                "start");

    }

    @Test
    public void testHelpMissingCommand() {
        testSimpleResponseMessage("Error: 'notarealcommand' is not a recognised command name.",
                "help", "notarealcommand");
    }

    @Test
    public void testHelpSpecificCommand() {
        String expectedResponseMessage =
                SIGIL+"start game-name\n" +
                "  Start a game\n" +
                "    game-name: Name of game to start";
        testSimpleResponseMessage(expectedResponseMessage,
                "help", "start");
    }

    private void testSimpleResponseMessage(String expectedResponseMessage, String... requestArgs) {
        LambdaHandler handler = new LambdaHandler();
        UserCommandRequest request = new UserCommandRequest(List.of(requestArgs), MessageChannel.STANDARD, DUMMY_USER_ID);
        UserCommandResponse actualResponse = handler.handleRequest(request, null);

        logger.info("request = " + Arrays.toString(requestArgs) + ", response = " + actualResponse);

        Assertions.assertEquals(expectedResponseMessage, actualResponse.getOptionalMessageContent());
    }

    // For quicker test generation: print command output and fail, so it can be manually inspected and added in test.
    @SuppressWarnings("unused")
    private void cheatGenerateOutput(String... requestArgs) {
        UserCommandResponse actualResponse = new LambdaHandler().handleRequest(
                new UserCommandRequest(List.of(requestArgs), MessageChannel.STANDARD, DUMMY_USER_ID),
                null);
        Assertions.fail("Cheat output: " + StringEscapeUtils.escapeJava(actualResponse.getOptionalMessageContent()));
    }

}
