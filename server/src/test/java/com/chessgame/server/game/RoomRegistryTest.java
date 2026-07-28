package com.chessgame.server.game;

import com.chessgame.server.repository.GameRepository;
import com.chessgame.server.repository.MoveHistoryRepository;
import com.chessgame.server.repository.UserRepository;
import com.chessgame.server.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RoomRegistryTest {

    private static final long GRACE_MS = 20_000;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveHistoryRepository moveHistoryRepository;

    private RoomRegistry newRegistry() {
        return new RoomRegistry(new RatingService(userRepository), messagingTemplate,
                gameRepository, moveHistoryRepository);
    }

    private static long later(long millis) {
        return System.currentTimeMillis() + millis;
    }

    /**
     * Runs the sweeper the way the 50ms ticker does: one pass notices the room
     * is empty and stamps it, a later pass finds the stamp old enough and
     * closes it.
     *
     * A single call can only ever stamp. closeDesertedRoomsAt uses putIfAbsent
     * with the very "now" it was handed, so on the first pass the elapsed time
     * is zero no matter how far in the future that "now" is - and join() has
     * already cleared any stamp from room creation. Three tests here called it
     * once and asserted the room was gone, which is why they were failing.
     */
    private static void sweepUntilClosed(RoomRegistry registry) {
        long desertedAt = System.currentTimeMillis();
        registry.closeDesertedRoomsAt(desertedAt);
        registry.closeDesertedRoomsAt(desertedAt + GRACE_MS + 1);
    }

    @Test
    void createdRoomsGetDistinctIds() {
        RoomRegistry registry = newRegistry();
        assertNotEquals(registry.createRoom().roomId(), registry.createRoom().roomId());
    }

    @Test
    void createdRoomCanBeLookedUp() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        assertTrue(registry.exists(roomId));
        assertTrue(registry.find(roomId).isPresent());
    }

    @Test
    void lookupIsCaseInsensitive() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        assertTrue(registry.find(roomId.toLowerCase()).isPresent());
    }

    @Test
    void joiningAnUnknownRoomIsRejected() {
        RoomRegistry registry = newRegistry();
        assertEquals(RoomRegistry.JoinOutcome.Status.ROOM_NOT_FOUND,
                registry.join("ZZZZZZ", "session-1", "alice").status());
    }

    @Test
    void firstJoinerBecomesWhiteAndSecondBecomesBlack() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();

        assertEquals(Seats.Role.WHITE, registry.join(roomId, "s1", "alice").role());
        assertEquals(Seats.Role.BLACK, registry.join(roomId, "s2", "bob").role());
        assertEquals(Seats.Role.VIEWER, registry.join(roomId, "s3", "carol").role());
    }

    @Test
    void aPlayerCannotBeInTwoRoomsAtOnce() {
        RoomRegistry registry = newRegistry();
        String first = registry.createRoom().roomId();
        String second = registry.createRoom().roomId();
        registry.join(first, "s1", "alice");

        RoomRegistry.JoinOutcome outcome = registry.join(second, "s2", "alice");

        assertEquals(RoomRegistry.JoinOutcome.Status.ALREADY_IN_ANOTHER_ROOM, outcome.status());
        assertEquals(first, outcome.occupiedRoomId());
    }

    @Test
    void rejoiningTheSameRoomFromANewSessionIsAllowed() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");

        RoomRegistry.JoinOutcome outcome = registry.join(roomId, "s2", "alice");

        assertTrue(outcome.isJoined());
        assertEquals(Seats.Role.WHITE, outcome.role());
    }

    @Test
    void afterLeavingAPlayerMayJoinADifferentRoom() {
        RoomRegistry registry = newRegistry();
        String first = registry.createRoom().roomId();
        String second = registry.createRoom().roomId();
        registry.join(first, "s1", "alice");
        registry.join(first, "s2", "bob");

        registry.leave("s1");

        assertTrue(registry.join(second, "s3", "alice").isJoined());
    }

    @Test
    void aSoloCreatorWhoRefreshesDoesNotLoseTheRoom() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");

        registry.leave("s1");
        registry.closeDesertedRoomsAt(later(GRACE_MS / 2));

        assertTrue(registry.exists(roomId));
        assertTrue(registry.join(roomId, "s2", "alice").isJoined());
    }

    @Test
    void aRoomWithNobodySeatedSurvivesTheGracePeriod() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();

        registry.closeDesertedRoomsAt(later(GRACE_MS / 2));

        assertTrue(registry.exists(roomId));
    }

    @Test
    void aRoomNobodyEverJoinedIsCleanedUp() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();

        registry.closeDesertedRoomsAt(later(GRACE_MS + 1));

        assertFalse(registry.exists(roomId));
    }

    @Test
    void roomStaysOpenWhileOnePlayerRemains() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");
        registry.join(roomId, "s2", "bob");

        registry.leave("s1");
        registry.closeDesertedRoomsAt(later(GRACE_MS + 1));

        assertTrue(registry.exists(roomId));
    }

    @Test
    void roomClosesOnceBothPlayersHaveBeenGoneForTheGracePeriod() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");
        registry.join(roomId, "s2", "bob");

        registry.leave("s1");
        registry.leave("s2");
        sweepUntilClosed(registry);

        assertFalse(registry.exists(roomId));
    }

    @Test
    void viewersDoNotKeepARoomAlive() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");
        registry.join(roomId, "s2", "bob");
        registry.join(roomId, "s3", "carol");

        registry.leave("s1");
        registry.leave("s2");
        sweepUntilClosed(registry);

        assertFalse(registry.exists(roomId));
    }

    @Test
    void closingARoomForgetsEverySessionItHeld() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");
        registry.join(roomId, "s2", "bob");
        // Carol has to be a viewer for this to be about closing a deserted
        // room. With only two joins she took the black seat, so the room was
        // never deserted and never closed.
        registry.join(roomId, "s3", "carol");

        registry.leave("s1");
        registry.leave("s2");
        sweepUntilClosed(registry);

        assertFalse(registry.exists(roomId));
        assertTrue(registry.roomForSession("s3").isEmpty());
        assertTrue(registry.usernameForSession("s3").isEmpty());
    }

    @Test
    void aSecondTabDoesNotCountAsLeaving() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");
        registry.join(roomId, "s2", "bob");
        registry.join(roomId, "s1b", "alice");

        registry.leave("s1");
        registry.closeDesertedRoomsAt(later(GRACE_MS + 1));

        assertTrue(registry.exists(roomId));
        assertTrue(registry.roomForSession("s1b").isPresent());
    }

    @Test
    void leavingAnUnknownSessionIsHarmless() {
        RoomRegistry registry = newRegistry();
        registry.leave("never-seen");
    }

    @Test
    void sessionsResolveToTheirRoomAndUser() {
        RoomRegistry registry = newRegistry();
        String roomId = registry.createRoom().roomId();
        registry.join(roomId, "s1", "alice");

        assertEquals(roomId, registry.roomForSession("s1").orElseThrow().roomId());
        assertEquals("alice", registry.usernameForSession("s1").orElseThrow());
    }
}
