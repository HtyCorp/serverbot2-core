package io.mamish.serverbot2.gamemetadata;

import com.google.gson.Gson;
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
    public void testBasicCreation() {

        GameMetadata submitted1 = new GameMetadata("game1", "New game (game1)!",
                GameReadyState.BUSY, null, null, null);
        GameMetadata submitted2 = new GameMetadata("game2", "New game (game2)!",
                GameReadyState.BUSY, null, null, null);

        GameMetadata return1 = handler.createGame(new CreateGameRequest(submitted1.getGameName(), submitted1.getFullName())).getNewMetadata();
        GameMetadata return2 = handler.createGame(new CreateGameRequest(submitted2.getGameName(), submitted2.getFullName())).getNewMetadata();

        assertEquals(submitted1, return1);
        assertEquals(submitted2, return2);

        List<GameMetadata> all = handler.listGames(new ListGamesRequest()).getGames();

        Assertions.assertEquals(2, all.size());

        all.forEach(m -> assertEqualsSome(m, submitted1, submitted2));

        GameMetadata describe1 = handler.describeGame(new DescribeGameRequest(submitted1.getGameName())).getGame();
        GameMetadata describe2 = handler.describeGame(new DescribeGameRequest(submitted2.getGameName())).getGame();

        assertEquals(submitted1, describe1);
        assertEquals(submitted2, describe2);

        GameMetadata updated = handler.updateGame(new UpdateGameRequest("game1", "My awesome updated game!",
                GameReadyState.RUNNING, "i-1234",
                null, null)).getNewMetadata();
        GameMetadata updateReference = new GameMetadata("game1", "My awesome updated game!",
                GameReadyState.RUNNING, "i-1234", null, null);
        GameMetadata updateDescribe = handler.describeGame(new DescribeGameRequest("game1")).getGame();

        assertEquals(updateReference, updated);
        assertEquals(updateReference, updateDescribe);

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
