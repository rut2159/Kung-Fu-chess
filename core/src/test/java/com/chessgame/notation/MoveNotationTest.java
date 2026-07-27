package com.chessgame.notation;

import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveNotationTest {

    private static final int STANDARD_ROWS = 8;

    private final MoveNotation notation = new MoveNotation(STANDARD_ROWS);

    private static MoveRecord move(Piece.Kind kind, Position source, Position destination, boolean capture) {
        return new MoveRecord(Piece.Color.WHITE, kind, source, destination, capture, 0);
    }

    @Test
    void formatTime_zero() {
        assertEquals("00:00.000", MoveNotation.formatTime(0));
    }

    @Test
    void formatTime_secondsAndMillis() {
        assertEquals("00:04.105", MoveNotation.formatTime(4105));
    }

    @Test
    void formatTime_overOneMinute() {
        assertEquals("01:05.432", MoveNotation.formatTime(65432));
    }

    @Test
    void formatMove_pawnNonCapture_noFilePrefix() {
        assertEquals("e4", notation.formatMove(
                move(Piece.Kind.PAWN, new Position(6, 4), new Position(4, 4), false)));
    }

    @Test
    void formatMove_pawnCapture_prefixesSourceFile() {
        assertEquals("exd6", notation.formatMove(
                move(Piece.Kind.PAWN, new Position(3, 4), new Position(2, 3), true)));
    }

    @Test
    void formatMove_knight_usesLetterN() {
        assertEquals("Nc3", notation.formatMove(
                move(Piece.Kind.KNIGHT, new Position(7, 1), new Position(5, 2), false)));
    }

    @Test
    void formatMove_bishopCapture_includesXMark() {
        assertEquals("Bxa6", notation.formatMove(
                move(Piece.Kind.BISHOP, new Position(7, 5), new Position(2, 0), true)));
    }

    @Test
    void formatMove_queen_startsWithQ() {
        assertTrue(notation.formatMove(
                move(Piece.Kind.QUEEN, new Position(7, 3), new Position(3, 3), false)).startsWith("Q"));
    }

    @Test
    void formatMove_rook_startsWithR() {
        assertTrue(notation.formatMove(
                move(Piece.Kind.ROOK, new Position(7, 0), new Position(5, 0), false)).startsWith("R"));
    }

    @Test
    void formatMove_king_startsWithK() {
        assertTrue(notation.formatMove(
                move(Piece.Kind.KING, new Position(7, 4), new Position(6, 4), false)).startsWith("K"));
    }

    @Test
    void formatMove_jumpCapture_appendsJumpMark() {
        MoveRecord record = new MoveRecord(Piece.Color.BLACK, Piece.Kind.ROOK,
                new Position(2, 3), new Position(2, 3), true, true, 0);
        assertEquals("Rxd6\u2191", notation.formatMove(record));
    }

    @Test
    void formatMove_ordinaryCapture_hasNoJumpMark() {
        String result = notation.formatMove(
                move(Piece.Kind.ROOK, new Position(7, 0), new Position(5, 0), true));
        assertEquals("Rxa3", result);
    }

    @Test
    void algebraic_rankFollowsBoardHeight() {
        MoveNotation tall = new MoveNotation(10);
        assertEquals("a10", tall.algebraic(new Position(0, 0)));
        assertEquals("a8", notation.algebraic(new Position(0, 0)));
    }

    @Test
    void constructor_rejectsNonPositiveHeight() {
        assertThrows(IllegalArgumentException.class, () -> new MoveNotation(0));
    }
}
