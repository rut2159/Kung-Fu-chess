package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopGameSessionTest {

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
    void wiresAllLayersTogether_soARealClickMoveActuallyWorks() {
        Board board = new BoardParser().parse("wR . .");
        DesktopGameSession session = new DesktopGameSession(board);

        session.controller.click(50, 50);
        session.controller.click(150, 50);
        session.gameEngine.wait(1000);

        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertNull(board.pieceAt(new Position(0, 0)));
    }

    @Test
    void exposesTheExactBoardItWasGiven_notACopy() {
        Board board = new BoardParser().parse("wK . .");
        DesktopGameSession session = new DesktopGameSession(board);

        assertSame(board, session.board);
    }

    @Test
    void aRealClickMove_reachesTheMoveLogSubscriber() {
        Board board = new BoardParser().parse("wR . .");
        DesktopGameSession session = new DesktopGameSession(board);

        session.controller.click(50, 50);
        session.controller.click(150, 50);
        session.gameEngine.wait(1000); // MoveMadeEvent now fires at arrival, not at the request itself

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[MOVE]"),
                "A real move made through click handling should reach MoveLogSubscriber");
    }
}
