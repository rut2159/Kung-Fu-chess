package com.chessgame.ui.moves;

import com.chessgame.engine.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveHistoryFormatterTest {

    @Test
    void formatTime_zero() {
        assertEquals("00:00.000", MoveHistoryFormatter.formatTime(0));
    }

    @Test
    void formatTime_secondsAndMillis() {
        assertEquals("00:04.105", MoveHistoryFormatter.formatTime(4105));
    }

    @Test
    void formatTime_overOneMinute() {
        assertEquals("01:05.432", MoveHistoryFormatter.formatTime(65432));
    }

    @Test
    void formatMove_pawnNonCapture_noFilePrefix() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(6, 4), new Position(4, 4), false, 0);
        assertEquals("e4", MoveHistoryFormatter.formatMove(move));
    }

    @Test
    void formatMove_pawnCapture_prefixesSourceFile() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(3, 4), new Position(2, 3), true, 0);
        assertEquals("exd6", MoveHistoryFormatter.formatMove(move));
    }

    @Test
    void formatMove_knight_usesLetterN() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.KNIGHT,
                new Position(7, 1), new Position(5, 2), false, 0);
        assertEquals("Nc3", MoveHistoryFormatter.formatMove(move));
    }

    @Test
    void formatMove_bishopCapture_includesXMark() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.BISHOP,
                new Position(7, 5), new Position(2, 0), true, 0);
        assertEquals("Bxa6", MoveHistoryFormatter.formatMove(move));
    }

    @Test
    void formatMove_queen_startsWithQ() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.QUEEN,
                new Position(7, 3), new Position(3, 3), false, 0);
        assertTrue(MoveHistoryFormatter.formatMove(move).startsWith("Q"));
    }

    @Test
    void formatMove_rook_startsWithR() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.ROOK,
                new Position(7, 0), new Position(5, 0), false, 0);
        assertTrue(MoveHistoryFormatter.formatMove(move).startsWith("R"));
    }

    @Test
    void formatMove_king_startsWithK() {
        MoveRecord move = new MoveRecord(Piece.Color.WHITE, Piece.Kind.KING,
                new Position(7, 4), new Position(6, 4), false, 0);
        assertTrue(MoveHistoryFormatter.formatMove(move).startsWith("K"));
    }
}
