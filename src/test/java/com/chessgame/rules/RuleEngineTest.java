package com.chessgame.rules;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleEngineTest / טסטים ל-RuleEngine
 *
 * בודק את כל 4 סיבות-הדחייה (outside_board, empty_source,
 * friendly_destination, illegal_piece_move) + מקרה תקין - בדיוק
 * מה שהמסמך מגדיר כ"reason" יציב וקריא-למכונה.
 */
class RuleEngineTest {

    private Board board;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        // BoardParser כבר בדוק בנפרד (BoardParserTest) - כאן אנחנו
        // סומכים עליו כדי לבנות מהר לוח-בדיקה קריא, במקום board.addPiece
        // ידני שוב ושוב
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
        // wR ב-(0,0) ל-wK ב-(2,0) - אותו צבע (לבן), אז friendly_destination
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(2, 0));

        assertFalse(result.isValid());
        assertEquals(MoveReason.FRIENDLY_DESTINATION, result.reason());
    }

    @Test
    void movingRookDiagonally_isRejectedAsIllegalPieceMove() {
        // צריח לא זז באלכסון - זה נכשל *לא* בגלל תפוסה/גבולות,
        // אלא כי זה סותר את חוק-התנועה של הכלי עצמו
        MoveValidation result = ruleEngine.validateMove(new Position(0, 0), new Position(1, 1));

        assertFalse(result.isValid());
        assertEquals(MoveReason.ILLEGAL_PIECE_MOVE, result.reason());
    }

    @Test
    void ruleEngine_neverMutatesTheBoard() {
        // read-only ביחס ל-Board: אחרי כמה קריאות ל-validateMove
        // (גם תקינות וגם לא), הלוח חייב להישאר בדיוק כמו שהיה
        Piece pieceBefore = board.pieceAt(new Position(0, 0));

        ruleEngine.validateMove(new Position(0, 0), new Position(0, 1));
        ruleEngine.validateMove(new Position(0, 0), new Position(2, 0));

        assertSame(pieceBefore, board.pieceAt(new Position(0, 0)));
        assertNull(board.pieceAt(new Position(0, 1))); // לא זז בפועל!
    }
}
