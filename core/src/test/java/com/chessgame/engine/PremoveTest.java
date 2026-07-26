package com.chessgame.engine;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.engine.snapshot.GameSnapshot;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PremoveTest {
    private GameEngine engineFor(Board board) {
        return new GameEngine(board, new GameState(), new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
    }

    @Test
    void requestingAMoveWhileCoolingDown_queuesAPremoveInsteadOfRejecting() {
        Board board = new BoardParser().parse("wR . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        MoveResult premove = engine.requestMove(new Position(0, 1), new Position(0, 2));

        assertFalse(premove.isAccepted(), "לא מתבצע מיד");
        assertEquals(MoveReason.PREMOVE_QUEUED, premove.reason());
        assertNotNull(board.pieceAt(new Position(0, 1)), "הכלי עדיין לא זז בפועל");
    }

    @Test
    void premoveFiresAutomatically_whenCooldownExpires() {
        Board board = new BoardParser().parse("wR . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        engine.requestMove(new Position(0, 1), new Position(0, 2));

        engine.wait(10000);
        engine.wait(1000);

        assertNull(board.pieceAt(new Position(0, 1)));
        assertNotNull(board.pieceAt(new Position(0, 2)), "הכלי זז אוטומטית ליעד השמור");
    }

    @Test
    void newPremoveRequest_overwritesThePreviousOne() {
        Board board = new BoardParser().parse("wR . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        engine.requestMove(new Position(0, 1), new Position(0, 2));
        engine.requestMove(new Position(0, 1), new Position(0, 3));

        engine.wait(10000);
        engine.wait(2000);

        assertNull(board.pieceAt(new Position(0, 1)));
        assertNull(board.pieceAt(new Position(0, 2)), "ה-premove הישן לא בוצע");
        assertNotNull(board.pieceAt(new Position(0, 3)), "רק ה-premove האחרון בוצע");
    }

    @Test
    void illegalMoveWhileCoolingDown_cancelsThePendingPremove() {
        Board board = new BoardParser().parse("wR . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        engine.requestMove(new Position(0, 1), new Position(0, 2));

        MoveResult cancel = engine.requestMove(new Position(0, 1), new Position(5, 5));
        assertFalse(cancel.isAccepted());
        assertEquals(MoveReason.OUTSIDE_BOARD, cancel.reason());

        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(0, 1)), "הכלי נשאר במקומו - ה-premove בוטל");
        assertNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void snapshot_exposesHasPremove_forUiHighlighting() {
        Board board = new BoardParser().parse("wR . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        assertFalse(pieceViewAt(engine, new Position(0, 1)).hasPremove(), "עדיין אין premove");

        engine.requestMove(new Position(0, 1), new Position(0, 2));

        assertTrue(pieceViewAt(engine, new Position(0, 1)).hasPremove(), "יש premove ממתין - ה-UI אמור לצבוע כחול");
    }

    private GameSnapshot.PieceView pieceViewAt(GameEngine engine, Position position) {
        return engine.snapshot(null).pieces().stream()
                .filter(p -> p.position().equals(position))
                .findFirst()
                .orElseThrow();
    }

    /**
     * אם עד שהקירור פג, היעד השמור של ה-premove כבר לא-חוקי (כאן: כלי
     * ידידותי אחר תפס את המשבצת בינתיים) - ה-premove פשוט נכשל בשקט
     * (בלי לקרוס), והכלי נשאר במקומו.
     */
    @Test
    void premoveThatBecomesIllegalByFireTime_failsSilentlyWithoutCrashing() {
        Board board = new BoardParser().parse("wR . . .\n. . wK .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        engine.requestMove(new Position(0, 1), new Position(0, 2));

        engine.requestMove(new Position(1, 2), new Position(0, 2));
        engine.wait(1000);

        assertDoesNotThrow(() -> engine.wait(9000), "הקירור פג וה-premove מנסה לרוץ - לא אמור לקרוס גם אם לא-חוקי כרגע");

        assertNotNull(board.pieceAt(new Position(0, 1)), "הרוק נשאר במקומו - ה-premove נכשל בשקט");
        Piece atTarget = board.pieceAt(new Position(0, 2));
        assertNotNull(atTarget);
        assertEquals(Piece.Kind.KING, atTarget.kind(), "המלך עדיין שם - הרוק לא הצליח לתפוס אותו");
    }
}
