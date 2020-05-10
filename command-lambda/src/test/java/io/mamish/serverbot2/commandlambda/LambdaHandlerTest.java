package io.mamish.serverbot2.commandlambda;

import com.google.gson.Gson;
import io.mamish.serverbot2.commandlambda.model.commands.ICommandHandler;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceRequest;
import io.mamish.serverbot2.commandlambda.model.service.CommandServiceResponse;
import io.mamish.serverbot2.commandlambda.model.service.ICommandService;
import io.mamish.serverbot2.discordrelay.model.service.MessageChannel;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class LambdaHandlerTest {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;
    private static final String DUMMY_USER_ID = "12345678901234567";

    private Logger logger = Logger.getLogger("LambdaHandlerTest");
    private Gson gson = new Gson();

    @Test
    public void testMissingCommand() {
        testSimpleResponseMessage("Error: 'notarealcommand' is not a recognised command.",
                "notarealcommand", "arg0", "arg1");
    }

    @Test
    public void testStartMissingArgument() {

        String expectedResponseMessage = "Error: Expected at least 1 argument but got 0."
                + "\nUsage: "+SIGIL+"start game-name"
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
                SIGIL+"start game-name\n" +
                "  Start a game\n" +
                "    game-name: Name of game to start";
        testSimpleResponseMessage(expectedResponseMessage,
                "help", "start");
    }

    private void testSimpleResponseMessage(String expectedResponseMessage, String... requestArgs) {
        LambdaHandler handler = new LambdaHandler();
        ICommandService localClient = ApiClient.localLambda(ICommandService.class, handler);

        CommandServiceRequest request = new CommandServiceRequest(List.of(requestArgs), MessageChannel.STANDARD, DUMMY_USER_ID);
        CommandServiceResponse response = localClient.requestUserCommand(request);

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
