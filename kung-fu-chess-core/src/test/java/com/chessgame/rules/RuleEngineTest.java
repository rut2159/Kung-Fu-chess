package com.chessgame.rules;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private Board board;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wR . bK\n. . .\nwK . .");
        ruleEngine = new RuleEngine(board, new PieceRules());
    }

    @Test
    void rookMovingInStraightLine_isValid() {
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(0, 1));

        assertTrue(result.isValid());
        assertEquals(MoveReason.OK, result.reason());
    }

    @Test
    void destinationOutsideBoard_isRejected() {
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(0, 99));

        assertFalse(result.isValid());
        assertEquals(MoveReason.OUTSIDE_BOARD, result.reason());
    }

    @Test
    void sourceOutsideBoard_isRejected() {
        MoveValidation result = ruleEngine.validateMove(new Position(-1, 0), new Position(0, 0));

        assertFalse(result.isValid());
        assertEquals(MoveReason.OUTSIDE_BOARD, result.reason());
    }

    @Test
    void movingFromAnEmptyCell_isRejected() {
        MoveValidation result = ruleEngine.validateMove(new Position(1, 1), new Position(1, 2));

        assertFalse(result.isValid());
        assertEquals(MoveReason.EMPTY_SOURCE, result.reason());
    }

    @Test
    void movingOntoAFriendlyPiece_isRejected() {
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(2, 0));

        assertFalse(result.isValid());
        assertEquals(MoveReason.FRIENDLY_DESTINATION, result.reason());
    }

    @Test
    void movingRookDiagonally_isRejectedAsIllegalPieceMove() {
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(1, 1));

        assertFalse(result.isValid());
        assertEquals(MoveReason.ILLEGAL_PIECE_MOVE, result.reason());
    }

    @Test
    void ruleEngine_neverMutatesTheBoard() {
        Piece pieceBefore = board.pieceAt(new Position(0, 0));

        ruleEngine.validateMove(new Position(0, 0), new Position(0, 1));
        ruleEngine.validateMove(new Position(0, 0), new Position(2, 0));

        assertSame(pieceBefore, board.pieceAt(new Position(0, 0)));
        assertNull(board.pieceAt(new Position(0, 1)));
    }
}
