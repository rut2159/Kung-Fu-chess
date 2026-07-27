package com.chessgame.server.service;

import com.chessgame.io.StandardBoard;
import com.chessgame.server.dto.GameStateMessage;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.game.Topics;
import com.chessgame.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DisconnectAbandonTest {

    private static final int COUNTDOWN_MS = 20_000;

    /** רגלי שחור מ-e7 ל-e5 על לוח פתיחה רגיל. */
    private static final MoveCommand BLACK_PAWN_OPENING = new MoveCommand(1, 4, 3, 4);

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private PlayerAssignmentService assignments;

    private GameService newGameWithTwoPlayers() {
        assignments = new PlayerAssignmentService();
        GameService gameService = new GameService(
                assignments, new RatingService(userRepository), messagingTemplate, StandardBoard.create());
        assignments.assign("white-session", "alice");
        assignments.assign("black-session", "bob");
        return gameService;
    }

    private GameStateMessage lastBroadcastState() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq(Topics.GAME_STATE), captor.capture());
        List<Object> sent = captor.getAllValues();
        return (GameStateMessage) sent.get(sent.size() - 1);
    }

    @Test
    void whenTheCountdownExpires_theGameEndsWithoutAWinner() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertTrue(state.gameOver());
        assertNull(state.winner(), "an abandoned game has no winner");
    }

    @Test
    void afterTheGameIsAbandoned_theClientStillKnowsWhoLeft() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertEquals("alice", state.disconnectedUsername());
        assertNull(state.resignInSeconds(), "the countdown is finished, so there is nothing left to show");
    }

    @Test
    void whileTheOpponentIsAway_theRemainingPlayerCannotMove() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        var result = gameService.handleMove(BLACK_PAWN_OPENING, "black-session");

        assertFalse(result.isAccepted(), "the game is frozen until the opponent returns or the game is abandoned");
    }

    @Test
    void afterTheOpponentReturns_theRemainingPlayerCanMoveAgain() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.join("alice-new-session", "alice");
        var result = gameService.handleMove(BLACK_PAWN_OPENING, "black-session");

        assertTrue(result.isAccepted());
    }

    @Test
    void beforeTheDeadline_theGameIsStillRunning() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.advanceTimeAndBroadcast(COUNTDOWN_MS - 1_000);

        assertFalse(lastBroadcastState().gameOver());
    }

    @Test
    void reconnectingInTime_cancelsTheCountdown() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.advanceTimeAndBroadcast(10_000);
        gameService.join("alice-new-session", "alice");
        gameService.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertFalse(state.gameOver());
        assertNull(state.disconnectedUsername());
    }

    @Test
    void remainingSecondsAreBroadcastToTheClients() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");
        gameService.advanceTimeAndBroadcast(5_000);

        GameStateMessage state = lastBroadcastState();
        assertEquals("alice", state.disconnectedUsername());
        assertEquals(15, state.resignInSeconds().intValue());
    }

    @Test
    void viewerDisconnecting_doesNotEndTheGame() {
        GameService gameService = newGameWithTwoPlayers();
        assignments.assign("viewer-session", "carol");

        gameService.playerDisconnected("carol");
        gameService.advanceTimeAndBroadcast(COUNTDOWN_MS);

        GameStateMessage state = lastBroadcastState();
        assertFalse(state.gameOver());
        assertNull(state.disconnectedUsername());
    }

    @Test
    void reconnectingPlayer_keepsTheSameColour() {
        GameService gameService = newGameWithTwoPlayers();

        gameService.playerDisconnected("alice");

        assertEquals(PlayerAssignmentService.Role.WHITE, gameService.join("alice-new-session", "alice"));
    }
}
