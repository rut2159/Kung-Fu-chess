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
        assertNull(board.pieceAt(new Position(0, 1))); // עוד לא הגיע ב-999ms

        kingCaptured |= arbiter.advanceTime(1); // עכשיו בדיוק 1000ms הצטברו
        assertNotNull(board.pieceAt(new Position(0, 1))); // הגיע!
        assertFalse(kingCaptured);
    }

    @Test
    void movingTwoSquares_takes2000ms() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 2));

        arbiter.advanceTime(1000);
        assertNull(board.pieceAt(new Position(0, 2))); // עדיין לא, מרחק 2 = 2000ms

        arbiter.advanceTime(1000); // סה"כ 2000ms
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
        // התוספת שלנו: ריבוי-תנועות. שני כלים לא-חוצים-מסלול,
        // זזים בו-זמנית, שניהם צריכים להגיע בהצלחה
        board = new BoardParser().parse("wR . . bR");
        arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        arbiter.startMotion(new Position(0, 3), new Position(0, 2));
        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 1))); // wR הגיע
        assertNotNull(board.pieceAt(new Position(0, 2))); // bR הגיע
    }

    @Test
    void canStartMotion_isFalseWhenSourcePieceIsAlreadyMoving() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        // אותו כלי, עוד לא הגיע - ניסיון-מהלך-נוסף על אותו מקור אמור להידחות
        assertFalse(arbiter.canStartMotion(new Position(0, 0), new Position(0, 2)));
    }

    @Test
    void jumpingPiece_capturesAnEnemyThatArrivesWhileAirborne() {
        // תוספת שלנו: קפיצה + לכידה-באוויר
        board = new BoardParser().parse("wK bR .");
        arbiter = new RealTimeArbiter(board);

        arbiter.startJump(new Position(0, 0));                        // wK קופץ, landTime=1000
        arbiter.startMotion(new Position(0, 1), new Position(0, 0));  // bR רץ אליו, מרחק 1, arrivalTime=1000

        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 0)));   // wK עדיין שם
        assertEquals(Piece.Kind.KING, board.pieceAt(new Position(0, 0)).kind());
        assertNull(board.pieceAt(new Position(0, 1)));       // bR נעלם - נלכד באוויר, לא הגיע בכלל
    }

    @Test
    void jumpingPiece_landsNormallyIfNoEnemyArrives() {
        arbiter.startJump(new Position(0, 0));

        arbiter.advanceTime(1000);

        // הכלי עדיין באותו מקום - אין שינוי בלוח, רק "נחת" (הפסיק להיות מרחף)
        assertNotNull(board.pieceAt(new Position(0, 0)));
        // ההוכחה שהוא "נחת": עכשיו מותר להתחיל בשבילו מהלך רגיל חדש
        assertTrue(arbiter.canStartMotion(new Position(0, 0), new Position(0, 1)));
    }
}
