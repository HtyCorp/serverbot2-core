package io.mamish.serverbot2.discordrelay;

import io.mamish.serverbot2.discordrelay.model.service.EditMessageRequest;
import io.mamish.serverbot2.discordrelay.model.service.IDiscordServiceHandler;
import io.mamish.serverbot2.discordrelay.model.service.NewMessageRequest;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.reflect.JsonRequestDispatcher;
import io.mamish.serverbot2.sharedutil.reflect.RequestHandlingRuntimeException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordServiceHandler implements IDiscordServiceHandler {

    private JsonRequestDispatcher<IDiscordServiceHandler> requestDispatcher;
    private MessageDynamoTable messageDynamoTable;

    private Logger logger;

    public DiscordServiceHandler() {
        requestDispatcher = new JsonRequestDispatcher<>(this, IDiscordServiceHandler.class);
        messageDynamoTable = new MessageDynamoTable();
        logger = Logger.getLogger("DiscordServiceHandler");
    }

    public void handleRequest(String messageBody) {
        try {
            requestDispatcher.dispatch(messageBody);
        } catch (RequestHandlingRuntimeException e) {
            logger.log(Level.WARNING, "Uncaught exception while handling discord relay request", e);
        }
    }

    @Override
    public void onRequestNewMessage(NewMessageRequest newMessageRequest) {

        DynamoMessageItem item = messageDynamoTable.getItem(newMessageRequest.getExternalId());
        if (item != null) {
            // This interface doesn't
            logger.warning("Received NewMessageRequest with an already used external ID");
        }

        String uuidString = UUID.randomUUID().toString();
        // TODO

    }

    @Override
    public void onRequestEditMessage(EditMessageRequest editMessageRequest) {



    }
}
