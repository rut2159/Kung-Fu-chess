package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the actual wiring done in GameSession.wireEventBusSubscribers():
 * subscribers must be registered BEFORE GameStartedEvent is published, or the
 * event is silently lost (see GameSession's constructor ordering).
 */
class GameSessionEventWiringTest {

    private PrintStream originalErr;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void redirectStdErr() {
        originalErr = System.err;
        captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdErr() {
        System.setErr(originalErr);
    }

    @Test
    void creatingASession_publishesGameStartedEventToAnAlreadyWiredSubscriber() {
        Board board = new BoardParser().parse("wK . .");

        new GameSession(board);

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("GAME STARTED"),
                "GameStartedEvent must be published AFTER subscribers are wired, or this is lost");
    }

    @Test
    void aRealMoveThroughTheEngine_reachesTheMoveLogSubscriber() {
        Board board = new BoardParser().parse("wR . .");
        GameSession session = new GameSession(board);

        session.gameEngine.requestMove(new com.chessgame.model.Position(0, 0), new com.chessgame.model.Position(0, 1));
        session.gameEngine.wait(1000); // MoveMadeEvent now fires at arrival, not at the request itself

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[MOVE]"),
                "A real move made through the engine should reach MoveLogSubscriber");
    }
}
