package io.mamish.serverbot2.gamemetadata;

import com.google.gson.Gson;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.framework.exception.server.RequestValidationException;
import io.mamish.serverbot2.gamemetadata.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.mamish.serverbot2.testutils.ReflectiveEquals.areEqual;

public class GameMetadataServiceHandlerTest {

    private GameMetadataServiceHandler handler;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        handler = new GameMetadataServiceHandler();
    }

    @Test
    public void testEmptyTable() {
        List<GameMetadata> allGames = handler.listGames(new ListGamesRequest()).getGames();
        Assertions.assertEquals(0, allGames.size());
    }

    @Test
    public void testValidCreateAndUpdate() {

        GameMetadata submitted1 = new GameMetadata("game1", "New game (game1)!",
                GameReadyState.BUSY, null, null, null);
        GameMetadata submitted2 = new GameMetadata("game2", "New game (game2)!",
                GameReadyState.BUSY, null, null, null);

        // Create both games and check that the new metadata matches.

        GameMetadata return1 = handler.createGame(new CreateGameRequest(submitted1.getGameName(), submitted1.getFullName())).getNewMetadata();
        GameMetadata return2 = handler.createGame(new CreateGameRequest(submitted2.getGameName(), submitted2.getFullName())).getNewMetadata();

        assertEquals(submitted1, return1);
        assertEquals(submitted2, return2);

        // Try creating an already existing game and make sure it fails.

        Assertions.assertThrows(RequestValidationException.class, () -> handler.createGame(
                new CreateGameRequest("game1", "Another new game!")
        ));

        // List all games and make sure we have a 2-length list where the objects equal one or the other of the above
        // (note this model isn't supposed to guarantee order though DynamoDB itself has some guarantees on scans)

        List<GameMetadata> all = handler.listGames(new ListGamesRequest()).getGames();
        Assertions.assertEquals(2, all.size());
        all.forEach(m -> assertEqualsSome(m, submitted1, submitted2));

        // Describe both games and check that each of them matches the initial data

        GameMetadata describe1 = handler.describeGame(new DescribeGameRequest(submitted1.getGameName())).getGame();
        GameMetadata describe2 = handler.describeGame(new DescribeGameRequest(submitted2.getGameName())).getGame();

        assertEquals(submitted1, describe1);
        assertEquals(submitted2, describe2);

        // Update some values and make sure data is consistent in the return and after describing thegame

        GameMetadata updated = handler.updateGame(new UpdateGameRequest("game1", "My awesome updated game!",
                GameReadyState.RUNNING, "i-1234",
                null, null)).getNewMetadata();
        GameMetadata updateReference = new GameMetadata("game1", "My awesome updated game!",
                GameReadyState.RUNNING, "i-1234", null, null);
        GameMetadata updateDescribe = handler.describeGame(new DescribeGameRequest("game1")).getGame();

        assertEquals(updateReference, updated);
        assertEquals(updateReference, updateDescribe);

    }

    @Test
    public void testDeletion() {

        // Reference metadata and deletion request
        GameMetadata metadata = new GameMetadata("gametodelete", "The game I'm deleting",
                GameReadyState.BUSY, null, null, null);
        DeleteGameRequest deleteRequest = new DeleteGameRequest(metadata.getGameName());

        // Ensure this fails if we attempt to delete a non-existent game. Create afterwards to continue test.

        Assertions.assertThrows(RequestValidationException.class, () -> handler.deleteGame(deleteRequest));

        GameMetadata created = handler.createGame(
                new CreateGameRequest(metadata.getGameName(), metadata.getFullName())
        ).getNewMetadata();

        assertEquals(metadata, created);

        // Set game to stopped/unlocked state to prevent update. Deletion attempt should fail.

        GameMetadata updatedActual = handler.updateGame(new UpdateGameRequest("gametodelete", null, GameReadyState.STOPPED,
                null, null, null)).getNewMetadata();
        GameMetadata updatedExpected = new GameMetadata("gametodelete", "The game I'm deleting", GameReadyState.STOPPED,
        null, null, null);
        assertEquals(updatedExpected, updatedActual);

        Assertions.assertThrows(RequestHandlingException.class, () -> handler.deleteGame(deleteRequest));

        // Finally, lock game so we can delete it. Then a final check that the returned metadata matches.

        handler.lockGame(new LockGameRequest(metadata.getGameName()));
        GameMetadata deleted = handler.deleteGame(new DeleteGameRequest(metadata.getGameName())).getLastMetadata();
        assertEquals(metadata, deleted);

    }

    private void assertEquals(Object o1, Object o2) {
        String message = "o1=" + gson.toJson(o1) + ", o2=" + gson.toJson(o2);
        Assertions.assertTrue(areEqual(o1, o2), message);
    }

    private void assertNotEquals(Object o1, Object o2) {
        String message = "o1=" + gson.toJson(o1) + ", o2=" + gson.toJson(o2);
        Assertions.assertFalse(areEqual(o1, o2), message);
    }

    private void assertEqualsSome(Object o1, Object... os) {
        String message = "o1=" + gson.toJson(o1) + ", os=" + gson.toJson(os);
        boolean someMatch = Arrays.stream(os).anyMatch(o -> areEqual(o1, o));
        Assertions.assertTrue(someMatch, message);
    }

}
