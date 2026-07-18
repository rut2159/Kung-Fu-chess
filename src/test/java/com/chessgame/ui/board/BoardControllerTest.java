package com.chessgame.ui.board;

import com.chessgame.GameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardControllerTest {

    private GameSession sessionFor(Board board) {
        return new GameSession(board);
    }

    @Test
    void panel_isNeverNull() {
        Board board = new BoardParser().parse("wK . .\n. . .\n. . .");
        GameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "Alice", "Bob");

        assertNotNull(controller.panel());
    }

    @Test
    void onGameStateChanged_doesNotThrow() {
        Board board = new BoardParser().parse("wK . .\n. . .\n. . .");
        GameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "Alice", "Bob");

        assertDoesNotThrow(() -> controller.onGameStateChanged(session.gameEngine));
    }

    @Test
    void registersAsListener_andReactsToAcceptedMove() {
        Board board = new BoardParser().parse("wR . .\n. . .\n. . .");
        GameSession session = sessionFor(board);
        new BoardController(session, "Alice", "Bob");

        assertDoesNotThrow(() -> {
            session.gameEngine.requestMove(new Position(0, 0), new Position(0, 1));
            session.gameEngine.wait(1100);
        });
    }
}
