package io.mamish.serverbot2.commandlambda;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandRequest;
import io.mamish.serverbot2.commandlambda.model.ProcessUserCommandResponse;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class HttpHandlerTest {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;
    private static final String DUMMY_USER_ID = "12345678901234567";
    private static final String DUMMY_USER_NAME = "TestyMcTester#9876";
    private static final String DUMMY_MESSAGE_ID = "23456789012345678";

    private final Logger logger = Logger.getLogger("LambdaHandlerTest");
    private final Gson gson = new Gson();

    @BeforeAll
    static void disableXray() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
    }

    @Test
    public void testMissingCommand() {
        testSimpleResponseMessage("Error: 'notarealcommand' is not a recognised command.",
                "notarealcommand", "arg0", "arg1");
    }

    @Test
    public void testStartMissingArgument() {

        String expectedResponseMessage = "Error: Expected at least 1 argument but got 0."
                + "\nUsage: "+SIGIL+"start gameName"
                + "\nUse '"+SIGIL+"help start' for details.";

        testSimpleResponseMessage(expectedResponseMessage,
                "start");

    }

    @Test
    public void testHelpMissingCommand() {
        testSimpleResponseMessage("Can't look up help: 'notarealcommand' is not a recognised command name.",
                "help", "notarealcommand");
    }

    @Test
    public void testHelpSpecificCommand() {
        String expectedResponseMessage =
                SIGIL+"start gameName\n" +
                "  Start a game\n" +
                "    gameName: Name of game to start";
        testSimpleResponseMessage(expectedResponseMessage,
                "help", "start");
    }

    private void testSimpleResponseMessage(String expectedResponseMessage, String... requestArgs) {
        RootCommandHandler commandHandler = new RootCommandHandler();

        ProcessUserCommandRequest request = new ProcessUserCommandRequest(
                List.of(requestArgs),
                MessageChannel.MAIN,
                DUMMY_MESSAGE_ID,
                DUMMY_USER_ID,
                DUMMY_USER_NAME);
        ProcessUserCommandResponse response = commandHandler.processUserCommand(request);

        logger.info("request = " + Arrays.toString(requestArgs) + ", response = " + gson.toJson(response));

        Assertions.assertEquals(expectedResponseMessage, response.getOptionalMessageContent());
    }

//    // For quicker test generation: print command output and fail, so it can be manually inspected and added in test.
//    @SuppressWarnings("unused")
//    private void cheatGenerateOutput(String... requestArgs) {
//        String requestString = gson.toJson(new CommandServiceRequest(List.of(requestArgs), MessageChannel.STANDARD, DUMMY_USER_ID));
//        String responseString = new LambdaHandler().handleRequest(requestString, null);
//        CommandServiceResponse actualResponse = gson.fromJson(responseString, CommandServiceResponse.class);
//        Assertions.fail("Cheat output: " + StringEscapeUtils.escapeJava(actualResponse.getOptionalMessageContent()));
//    }

}
