package com.chessgame;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class GameSessionTest {

    @Test
    void exposesTheExactBoardItWasGiven_notACopy() {
        Board board = new BoardParser().parse("wK . .");
        GameSession session = new GameSession(board);

        assertSame(board, session.board);
    }
}
