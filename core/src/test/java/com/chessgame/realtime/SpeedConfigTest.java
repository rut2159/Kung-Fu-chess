package com.chessgame.realtime;

import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedConfigTest {
    @Test
    void standardPreset_matchesTheDocumentedValues() {
        assertEquals(1000, SpeedConfig.STANDARD.cellDurationMs());
        assertEquals(10_000, SpeedConfig.STANDARD.longCooldownMs());
    }

    @Test
    void lightningPreset_matchesTheDocumentedValues() {
        assertEquals(200, SpeedConfig.LIGHTNING.cellDurationMs());
        assertEquals(2_000, SpeedConfig.LIGHTNING.longCooldownMs());
    }

    @Test
    void lightningMotion_completesInOneFifthOfStandardTime() {
        Board board = new BoardParser().parse("wR . .");
        RealTimeArbiter arbiter = new RealTimeArbiter(board, SpeedConfig.LIGHTNING);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        arbiter.advanceTime(199);
        assertNull(board.pieceAt(new Position(0, 1)), "עדיין לא הגיע - 200ms/משבצת ב-Lightning");

        arbiter.advanceTime(1);
        assertNotNull(board.pieceAt(new Position(0, 1)));
    }

    @Test
    void lightningCooldown_expiresAfter2SecondsInsteadOf10() {
        Board board = new BoardParser().parse("wR . .");
        RealTimeArbiter arbiter = new RealTimeArbiter(board, SpeedConfig.LIGHTNING);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        arbiter.advanceTime(200);

        assertFalse(arbiter.canStartMotion(new Position(0, 1), new Position(0, 2)), "עדיין בקירור");

        arbiter.advanceTime(1999);
        assertFalse(arbiter.canStartMotion(new Position(0, 1), new Position(0, 2)), "עוד לא עברו 2000ms מתחילת הקירור");

        arbiter.advanceTime(1);
        assertTrue(arbiter.canStartMotion(new Position(0, 1), new Position(0, 2)), "2000ms עברו - קירור פג");
    }

    @Test
    void defaultConstructor_stillBehavesLikeStandard_backwardCompatible() {
        Board board = new BoardParser().parse("wR . .");
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertFalse(arbiter.canStartMotion(new Position(0, 1), new Position(0, 2)), "Standard - 10 שניות קירור, עדיין פעיל");
    }
}
