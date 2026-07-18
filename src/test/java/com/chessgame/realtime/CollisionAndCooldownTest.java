package com.chessgame.realtime;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.MoveResult;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionAndCooldownTest {

    private GameEngine engineFor(Board board) {
        return new GameEngine(board, new GameState(), new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
    }

    @Test
    void enemyCollision_earlierStartWins_andContinuesToOriginalDestination() {
        Board board = new BoardParser().parse("wR . . . . . . bR");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 4));
        engine.wait(500);
        engine.requestMove(new Position(0, 7), new Position(0, 3));
        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(0, 4)));
        assertEquals(Piece.Kind.ROOK, board.pieceAt(new Position(0, 4)).kind());
        for (int col = 0; col < 8; col++) {
            if (col != 4) assertNull(board.pieceAt(new Position(0, col)));
        }
    }

    @Test
    void friendlyCollision_laterArrivalStopsOneCellBefore() {
        Board board = new BoardParser().parse(
                ". . . . wR . . .\n. . . . . . . .\n. . . . . . . .\nwQ . . . . . . .\n" +
                        ". . . . . . . .\n. . . . . . . .\n. . . . . . . .\n. . . . . . . ."
        );
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 4), new Position(7, 4));
        engine.requestMove(new Position(3, 0), new Position(3, 7));

        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(7, 4)), "wR (מגיע-קודם) ממשיך ליעד-המקורי");
        assertNotNull(board.pieceAt(new Position(3, 3)), "wQ (מגיעה-מאוחר) נעצרת משבצת-לפני");
    }

    @Test
    void knightsNeverCollideWithEachOther() {
        Board board = new BoardParser().parse("wN . . . . . . bN\n. . . . . . . .\n. . . . . . . .\n. . . . . . . .");
        GameEngine engine = engineFor(board);

        MoveResult r1 = engine.requestMove(new Position(0, 0), new Position(2, 1));
        MoveResult r2 = engine.requestMove(new Position(0, 7), new Position(2, 6));

        assertTrue(r1.isAccepted());
        assertTrue(r2.isAccepted());
    }

    @Test
    void thirdPartyKillingAPiece_invalidatesItsStalePendingCollision() {
        Board board = new BoardParser().parse(". . . . .\n. . . . .\n. . . . .");
        board.addPiece(new Piece("g", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(0, 0)));
        board.addPiece(new Piece("a", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(2, 1)));
        board.addPiece(new Piece("b", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(1, 4)));
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 3));
        engine.requestMove(new Position(2, 1), new Position(0, 1));
        engine.requestMove(new Position(1, 4), new Position(1, 0));

        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(0, 3)), "ג' ניצח, המשיך ליעד-המקורי");
        assertNull(board.pieceAt(new Position(2, 1)));
        assertNull(board.pieceAt(new Position(0, 1)));
        assertNotNull(board.pieceAt(new Position(1, 0)), "ב' לא-הושפע כלל - ההתנגשות-הישנה-עם-א' דולגה");
    }

    @Test
    void stationaryEnemyBlocker_isCapturedMidPath_movingPieceContinues() {
        Board board = new BoardParser().parse("wR bP .");
        GameEngine engine = engineFor(board);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(0, 2));
        assertTrue(result.isAccepted());

        engine.wait(2000);

        assertNotNull(board.pieceAt(new Position(0, 2)));
        assertEquals(Piece.Kind.ROOK, board.pieceAt(new Position(0, 2)).kind());
        assertNull(board.pieceAt(new Position(0, 1)), "האויב-הדומם-באמצע-המסלול נהרג");
    }

    @Test
    void knightLandingOnFriendlyPiece_capturesItsOwnSide() {
        Board board = new BoardParser().parse(". . .\n. . wP\n. . .");
        board.addPiece(new Piece("n", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(0, 0)));
        GameEngine engine = engineFor(board);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 2));
        assertTrue(result.isAccepted());

        engine.wait(2000);

        Piece atTarget = board.pieceAt(new Position(1, 2));
        assertNotNull(atTarget);
        assertEquals(Piece.Kind.KNIGHT, atTarget.kind());
        assertNull(board.pieceAt(new Position(0, 0)));
    }

    @Test
    void cooldown_blocksImmediateSecondMove_thenAllowsAfterExpiry() {
        Board board = new BoardParser().parse("wR . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        MoveResult tooSoon = engine.requestMove(new Position(0, 1), new Position(0, 2));
        assertFalse(tooSoon.isAccepted(), "עדיין בקירור - נדחה");

        engine.wait(1000);
        MoveResult afterCooldown = engine.requestMove(new Position(0, 1), new Position(0, 2));
        assertTrue(afterCooldown.isAccepted(), "קירור פג - מותר");
    }

    @Test
    void friendlyBlockerInPath_makesTheMoveIllegal() {
        Board board = new BoardParser().parse("wR wP .");
        GameEngine engine = engineFor(board);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(0, 2));

        assertFalse(result.isAccepted());
        engine.wait(2000);
        assertEquals("wR wP .", new com.chessgame.io.BoardPrinter().print(board));
    }
}
