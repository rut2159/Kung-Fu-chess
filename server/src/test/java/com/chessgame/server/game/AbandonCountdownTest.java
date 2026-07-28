package com.chessgame.server.game;

import com.chessgame.io.StandardBoard;
import com.chessgame.model.Piece;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.repository.GameRepository;
import com.chessgame.server.repository.MoveHistoryRepository;
import com.chessgame.server.repository.User;
import com.chessgame.server.repository.UserRepository;
import com.chessgame.server.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbandonCountdownTest {

    private static final String ROOM_ID = "K7F2QX";
    private static final int COUNTDOWN_MS = 20_000;
    private static final MoveCommand BLACK_PAWN_OPENING = new MoveCommand(1, 4, 3, 4);

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveHistoryRepository moveHistoryRepository;

    private GameRoom newRoomWithTwoPlayers() {
        GameRoom room = new GameRoom(ROOM_ID, StandardBoard.create(),
                new RatingService(userRepository), messagingTemplate, gameRepository, moveHistoryRepository);
        room.join("alice");
        room.join("bob");
        return room;
    }

    private GameStateMessage lastBroadcastState() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq(Topics.gameState(ROOM_ID)), captor.capture());
        List<Object> sent = captor.getAllValues();
        return (GameStateMessage) sent.get(sent.size() - 1);
    }

    /**
     * "Auto-resign after 20 sec": a resignation hands the game to the opponent.
     * The engine still reports no winner - both kings are alive - so this is
     * the room awarding it, which is why the assertion is on the broadcast
     * state rather than on the snapshot.
     */
    @Test
    void whenTheCountdownExpiresTheOpponentWins() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertTrue(state.gameOver());
        assertEquals(Piece.Color.BLACK.name(), state.winner());
    }

    @Test
    void aForfeitIsRatedAsAWinForThePlayerWhoStayed() {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new User(1L, "alice", "hash", 1200)));
        when(userRepository.findByUsername("bob"))
                .thenReturn(Optional.of(new User(2L, "bob", "hash", 1200)));
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        ArgumentCaptor<Integer> alice = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> bob = ArgumentCaptor.forClass(Integer.class);
        verify(userRepository).updateRating(eq("alice"), alice.capture());
        verify(userRepository).updateRating(eq("bob"), bob.capture());
        assertTrue(alice.getValue() < 1200, "the player who walked away should lose rating");
        assertTrue(bob.getValue() > 1200, "the player who stayed should gain rating");
    }

    /**
     * A room whose second seat was never taken has no game to forfeit. Counting
     * down would destroy it under whoever arrives next with the code.
     */
    @Test
    void leavingBeforeTheOpponentArrivesReleasesTheSeatInsteadOfStartingACountdown() {
        GameRoom room = new GameRoom(ROOM_ID, StandardBoard.create(),
                new RatingService(userRepository), messagingTemplate, gameRepository, moveHistoryRepository);
        room.join("alice");

        room.playerLeft("alice");

        GameStateMessage state = lastBroadcastState();
        assertNull(state.whiteUsername());
        assertNull(state.disconnectedUsername());
        assertNull(state.resignInSeconds());
        assertFalse(state.gameOver());
    }

    @Test
    void theFreedSeatGoesToTheNextPlayerToArrive() {
        GameRoom room = new GameRoom(ROOM_ID, StandardBoard.create(),
                new RatingService(userRepository), messagingTemplate, gameRepository, moveHistoryRepository);
        room.join("alice");
        room.playerLeft("alice");

        assertEquals(Seats.Role.WHITE, room.join("bob"));
    }

    @Test
    void aRoomThatNeverStartedIsNotRated() {
        GameRoom room = new GameRoom(ROOM_ID, StandardBoard.create(),
                new RatingService(userRepository), messagingTemplate, gameRepository, moveHistoryRepository);
        room.join("alice");

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        verify(userRepository, never()).updateRating(anyString(), anyInt());
    }

    @Test
    void afterAbandonmentTheClientStillKnowsWhoLeft() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertEquals("alice", state.disconnectedUsername());
        assertNull(state.resignInSeconds());
    }

    @Test
    void whileTheOpponentIsAwayTheRemainingPlayerCannotMove() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");

        assertFalse(room.handleMove(BLACK_PAWN_OPENING, "bob").isAccepted());
    }

    @Test
    void afterTheOpponentReturnsTheRemainingPlayerCanMoveAgain() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.join("alice");

        assertTrue(room.handleMove(BLACK_PAWN_OPENING, "bob").isAccepted());
    }

    @Test
    void beforeTheDeadlineTheGameIsStillRunning() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS - 1_000);

        assertFalse(lastBroadcastState().gameOver());
    }

    @Test
    void returningInTimeCancelsTheCountdown() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(10_000);
        room.join("alice");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertFalse(state.gameOver());
        assertNull(state.disconnectedUsername());
    }

    @Test
    void remainingSecondsAreBroadcastToTheClients() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");
        room.advanceTimeAndBroadcast(5_000);

        GameStateMessage state = lastBroadcastState();
        assertEquals("alice", state.disconnectedUsername());
        assertEquals(15, state.resignInSeconds().intValue());
    }

    @Test
    void aViewerLeavingDoesNotEndTheGame() {
        GameRoom room = newRoomWithTwoPlayers();
        room.join("carol");

        room.playerLeft("carol");
        room.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertFalse(state.gameOver());
        assertNull(state.disconnectedUsername());
    }

    @Test
    void returningPlayerKeepsTheSameColour() {
        GameRoom room = newRoomWithTwoPlayers();

        room.playerLeft("alice");

        assertEquals(Seats.Role.WHITE, room.join("alice"));
    }
}
