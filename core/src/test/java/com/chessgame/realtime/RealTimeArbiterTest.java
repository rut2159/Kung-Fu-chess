package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RealTimeArbiterTest {
    private Board board;
    private RealTimeArbiter arbiter;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wR . .\n. . .\n. . .");
        arbiter = new RealTimeArbiter(board);
    }

    @Test
    void movingOneSquare_takes1000ms() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        boolean kingCaptured = arbiter.advanceTime(999);
        assertNull(board.pieceAt(new Position(0, 1)));

        kingCaptured |= arbiter.advanceTime(1);
        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertFalse(kingCaptured);
    }

    @Test
    void movingTwoSquares_takes2000ms() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 2));

        arbiter.advanceTime(1000);
        assertNull(board.pieceAt(new Position(0, 2)));

        arbiter.advanceTime(1000);
        assertNotNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void capturingAnEnemyPiece_reportsKingCaptureWhenTargetIsKing() {
        board = new BoardParser().parse("wR bK");
        arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        boolean kingCaptured = arbiter.advanceTime(1000);

        assertTrue(kingCaptured);
    }

    @Test
    void twoSimultaneousMotions_bothArriveIndependently() {
        board = new BoardParser().parse("wR . . bR");
        arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        arbiter.startMotion(new Position(0, 3), new Position(0, 2));
        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertNotNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void canStartMotion_isFalseWhenSourcePieceIsAlreadyMoving() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        assertFalse(arbiter.canStartMotion(new Position(0, 0), new Position(0, 2)));
    }

    @Test
    void jumpingPiece_capturesAnEnemyThatArrivesWhileAirborne() {
        board = new BoardParser().parse("wK bR .");
        arbiter = new RealTimeArbiter(board);

        arbiter.startJump(new Position(0, 0));
        arbiter.startMotion(new Position(0, 1), new Position(0, 0));

        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 0)));
        assertEquals(Piece.Kind.KING, board.pieceAt(new Position(0, 0)).kind());
        assertNull(board.pieceAt(new Position(0, 1)));
    }

    @Test
    void jumpingPiece_landsNormallyIfNoEnemyArrives() {
        arbiter.startJump(new Position(0, 0));

        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 0)));
        assertFalse(arbiter.canStartMotion(new Position(0, 0), new Position(0, 1)));

        arbiter.advanceTime(400);
        assertTrue(arbiter.canStartMotion(new Position(0, 0), new Position(0, 1)));
    }

    /**
     * advanceTime מפרק קפיצות-זמן גדולות לצעדים פנימיים קטנים (ראו
     * MAX_STEP_MS) - כדי שהתוצאה תהיה זהה בלי קשר לאיך שקוראים לו.
     * הטסט הזה משווה בין קריאה-ענקית-אחת (wait(10000)) לבין הרבה קריאות
     * קטנות (wait(500) שוב ושוב) על אותו תרחיש בדיוק, ומוודא שהתוצאה זהה.
     * זה בדיוק התרחיש שנכשל לפני התיקון (חוסם-ידידותי שמתפנה, ורוק
     * שממשיך ליעד המקורי - רק תלוי-גודל-קפיצה לפני התיקון).
     */
    @Test
    void advanceTime_bigJumpAndManySmallTicks_produceTheSameResult() {
        Board bigJumpBoard = new BoardParser().parse("wR . wK . .\n. . . . .");
        RealTimeArbiter bigJumpArbiter = new RealTimeArbiter(bigJumpBoard);
        bigJumpArbiter.startMotion(new Position(0, 0), new Position(0, 4));
        bigJumpArbiter.startMotion(new Position(0, 2), new Position(1, 2));
        bigJumpArbiter.advanceTime(10000);

        Board smallTicksBoard = new BoardParser().parse("wR . wK . .\n. . . . .");
        RealTimeArbiter smallTicksArbiter = new RealTimeArbiter(smallTicksBoard);
        smallTicksArbiter.startMotion(new Position(0, 0), new Position(0, 4));
        smallTicksArbiter.startMotion(new Position(0, 2), new Position(1, 2));
        for (int i = 0; i < 20; i++) {
            smallTicksArbiter.advanceTime(500);
        }

        assertEquals(
                bigJumpBoard.pieceAt(new Position(0, 4)) != null,
                smallTicksBoard.pieceAt(new Position(0, 4)) != null,
                "רוק מגיע ליעד - זהה בין קפיצה גדולה לטיקים קטנים"
        );
        assertNotNull(bigJumpBoard.pieceAt(new Position(0, 4)), "רוק ממשיך ליעד המקורי - החוסם התפנה בזמן");
    }
}
