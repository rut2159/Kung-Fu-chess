package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    @Test
    void wiresAllLayersTogether_soARealMoveActuallyWorks() {
        Board board = new BoardParser().parse("wR . .");
        GameSession session = new GameSession(board);

        session.controller.click(50, 50);
        session.controller.click(150, 50);
        session.gameEngine.wait(1000);

        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertNull(board.pieceAt(new Position(0, 0)));
    }

    @Test
    void exposesTheExactBoardItWasGiven_notACopy() {
        Board board = new BoardParser().parse("wK . .");
        GameSession session = new GameSession(board);

        assertSame(board, session.board);
    }
}
