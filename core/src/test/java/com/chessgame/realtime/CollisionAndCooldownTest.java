package com.chessgame.realtime;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.io.BoardParser;
import com.chessgame.io.StandardBoard;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionAndCooldownTest {
    private GameEngine engineFor(Board board) {
        return new GameEngine(board, new GameState(), new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
    }

    @Test
    void enemyCollision_firstMoverWins_andContinuesToOriginalDestination() {
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

    /**
     * שחזור מדויק של התרחיש שדווח: רץ שחור מ-g6 (6,6) ל-d3 (3,3), מלכה
     * לבנה מ-g4 (4,6) ל-e4 (4,4). המלכה מתחילה 1.76 שנייה אחרי הרץ - בפועל
     * הרץ כבר עבר את e4 ומזמן, אז אין שום התנגשות אמיתית. לפני התיקון,
     * ה-EnemyMotionCollision הישן (מבוסס startTime בלבד, בלי בדיקת-מרחק
     * רציפה) היה הורג את המלכה מיידית - זה בדיוק הבאג שתוקן.
     */
    @Test
    void enemyPathsCrossLongAfterOneAnother_bothArriveSafely() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("b", Piece.Color.BLACK, Piece.Kind.BISHOP, new Position(6, 6)));
        board.addPiece(new Piece("q", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 6)));
        GameEngine engine = engineFor(board);

        Position bishopDest = new Position(3, 3);
        Position queenDest = new Position(4, 4);

        engine.requestMove(new Position(6, 6), bishopDest);
        engine.wait(1760);
        engine.requestMove(new Position(4, 6), queenDest);
        engine.wait(10000);

        assertNotNull(board.pieceAt(bishopDest), "הרץ הגיע ליעד שלו בשלום");
        assertEquals(Piece.Kind.BISHOP, board.pieceAt(bishopDest).kind());
        assertNotNull(board.pieceAt(queenDest), "המלכה הגיעה ליעד שלה בשלום - לא נהרגה בדרך");
        assertEquals(Piece.Kind.QUEEN, board.pieceAt(queenDest).kind());
    }

    /**
     * אותו מסלול בדיוק, אבל שני הכלים מתחילים באותו רגע (startTime שווה
     * בדיוק) - עכשיו כן נכנסים לטווח ה-0.4 משבצת בו-זמנית, וקורית התנגשות
     * אמיתית. בשוויון מדויק, הכלי שכבר היה רשום כ"פעיל" (הרץ, שהוזז ראשון
     * בקוד) מנצח - כרונולוגית הוא תמיד הראשון.
     */
    @Test
    void enemyPathsCrossSimultaneously_theOneAlreadyActiveWinsTheTie() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("b", Piece.Color.BLACK, Piece.Kind.BISHOP, new Position(6, 6)));
        board.addPiece(new Piece("q", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 6)));
        GameEngine engine = engineFor(board);

        Position bishopDest = new Position(3, 3);
        Position queenDest = new Position(4, 4);
        Position queenSrc = new Position(4, 6);

        engine.requestMove(new Position(6, 6), bishopDest);
        engine.requestMove(queenSrc, queenDest);
        engine.wait(10000);

        assertNotNull(board.pieceAt(bishopDest), "הרץ ניצח - ממשיך ליעד המקורי שלו");
        assertNull(board.pieceAt(queenDest), "המלכה לא הגיעה ליעד - נהרגה בדרך");
        assertNull(board.pieceAt(queenSrc), "המלכה גם לא נשארה במקור - היא נהרסה לגמרי");
    }

    /**
     * פער-זמן קטן יותר (400ms, עדיין בתוך טווח ההתנגשות) - הרץ עדיין מקדים.
     * לפי "יתרון היוזמה" (כלל 1), הרץ מנצח שוב - בלי קשר לגיאומטריה של מי
     * "קרוב יותר" בפועל לנקודת המפגש. מי שהתחיל קודם מנצח, נקודה.
     */
    @Test
    void enemyPathsCrossWithSmallGap_theOneWhoStartedFirstStillWins() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("b", Piece.Color.BLACK, Piece.Kind.BISHOP, new Position(6, 6)));
        board.addPiece(new Piece("q", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 6)));
        GameEngine engine = engineFor(board);

        Position bishopDest = new Position(3, 3);
        Position queenDest = new Position(4, 4);
        Position queenSrc = new Position(4, 6);

        engine.requestMove(new Position(6, 6), bishopDest);
        engine.wait(400);
        engine.requestMove(queenSrc, queenDest);
        engine.wait(10000);

        assertNotNull(board.pieceAt(bishopDest), "הרץ התחיל קודם - ממשיך ליעד המקורי שלו");
        assertEquals(Piece.Kind.BISHOP, board.pieceAt(bishopDest).kind());
        assertNull(board.pieceAt(queenDest), "המלכה לא הגיעה ליעד - נהרגה בדרך");
        assertNull(board.pieceAt(queenSrc), "המלכה גם לא נשארה במקור - היא נהרסה לגמרי");
    }

    /**
     * אותו תרחיש גיאומטרי בדיוק, אבל הפעם המלכה היא שמתחילה קודם (ואז
     * הרץ, 200ms אחריה - לא 400! הגיאומטריה לא סימטרית בין שני הכיוונים,
     * כי המלכה והרץ נעים במרחקים/כיוונים שונים, אז חלון-ההתנגשות בפועל
     * שונה בכל כיוון - וידאתי את זה עם סריקה על כמה פערים). עכשיו המלכה
     * מנצחת - זה מוכיח שהניצחון תלוי אך ורק ב"מי התחיל קודם", לא בזהות
     * הכלי (מלכה/רץ) ולא במרחק/מהירות שלו.
     */
    @Test
    void enemyPathsCrossWithSmallGap_reversedOrder_theFirstMoverStillWinsRegardlessOfWhichSide() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("b", Piece.Color.BLACK, Piece.Kind.BISHOP, new Position(6, 6)));
        board.addPiece(new Piece("q", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 6)));
        GameEngine engine = engineFor(board);

        Position bishopDest = new Position(3, 3);
        Position bishopSrc = new Position(6, 6);
        Position queenDest = new Position(4, 4);

        engine.requestMove(new Position(4, 6), queenDest);
        engine.wait(200);
        engine.requestMove(bishopSrc, bishopDest);
        engine.wait(10000);

        assertNotNull(board.pieceAt(queenDest), "המלכה התחילה קודם - ממשיכה ליעד המקורי שלה");
        assertEquals(Piece.Kind.QUEEN, board.pieceAt(queenDest).kind());
        assertNull(board.pieceAt(bishopDest), "הרץ לא הגיע ליעד - נהרג בדרך");
        assertNull(board.pieceAt(bishopSrc), "הרץ גם לא נשאר במקור - הוא נהרס לגמרי");
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

    /**
     * שחזור מדויק של הבאג שדווח: פאון זז e2->e3, ומיד (בלי לחכות שהוא יגיע
     * בפועל!) מלכה זזה d1->f3 - נתיב שעובר דרך e2, המשבצת שהפאון *עדיין
     * "בדרך-החוצה" ממנה (לא הגיע בפועל ל-e3 עדיין). לפני התיקון, ה-פאון
     * נחשב "הגיע" ל-e2 (משבצת-המוצא-שלו-עצמו) מיידית ברגע ההתחלה - מה
     * שגרם למלכה (שמגיעה-לשם-רק-מאוחר-יותר) להיחשב "המאוחרת" ולהיעצר
     * מיד ליד המוצא שלה-עצמה, בלי לזוז בכלל.
     */
    @Test
    void friendlyPieceCrossingThroughAnotherFriendlyPiecesJustVacatedSquare_isNotBlocked() {
        Board board = StandardBoard.create();
        GameEngine engine = engineFor(board);

        Position e2 = new Position(6, 4), e3 = new Position(5, 4);
        Position d1 = new Position(7, 3), f3 = new Position(5, 5);

        engine.requestMove(e2, e3);
        engine.requestMove(d1, f3);

        engine.wait(3000);

        assertNull(board.pieceAt(e2));
        assertNotNull(board.pieceAt(e3), "הפאון הגיע ל-e3 כרגיל");
        assertNull(board.pieceAt(d1), "המלכה לא נשארה תקועה במוצא שלה");
        assertNotNull(board.pieceAt(f3), "המלכה הגיעה ל-f3 - לא נחסמה ע\"י המוצא של הפאון");
        assertEquals(Piece.Kind.QUEEN, board.pieceAt(f3).kind());
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
    void perpendicularEnemyPathsNeverActuallyGetClose_soFriendlyCollisionIsReal() {
        Board board = new BoardParser().parse(". . . . .\n. . . . .\n. . . . .");
        board.addPiece(new Piece("g", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(0, 0)));
        board.addPiece(new Piece("a", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(2, 1)));
        board.addPiece(new Piece("b", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(1, 4)));
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 3));
        engine.requestMove(new Position(2, 1), new Position(0, 1));
        engine.requestMove(new Position(1, 4), new Position(1, 0));

        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(0, 3)), "ג' ממשיך ליעד-המקורי שלו - לא הייתה שום התנגשות אמיתית עם א'");
        assertNotNull(board.pieceAt(new Position(0, 1)), "א' שורד ומגיע ליעד המקורי שלו - מסלוליו וג' מעולם לא התקרבו ל-0.4 משבצת");
        assertNull(board.pieceAt(new Position(2, 1)));
        assertNotNull(board.pieceAt(new Position(1, 2)), "ב' נעצר משבצת-לפני התנגשות ידידותית אמיתית (לא stale) עם א'");
        assertNull(board.pieceAt(new Position(1, 0)), "ב' לא הגיע ליעד - נעצר בדרך בגלל א'");
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
    void knightCannotLandOnFriendlyOccupiedSquare() {
        Board board = new BoardParser().parse(". . .\n. . wP\n. . .");
        board.addPiece(new Piece("n", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(0, 0)));
        GameEngine engine = engineFor(board);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 2));
        assertFalse(result.isAccepted());
        assertEquals(MoveReason.FRIENDLY_DESTINATION, result.reason());

        engine.wait(2000);

        Piece atTarget = board.pieceAt(new Position(1, 2));
        assertNotNull(atTarget);
        assertEquals(Piece.Kind.PAWN, atTarget.kind());

        Piece atSource = board.pieceAt(new Position(0, 0));
        assertNotNull(atSource);
        assertEquals(Piece.Kind.KNIGHT, atSource.kind());
    }

    /**
     * חשוב: מאז שהוספנו Premove, בקשת-מהלך תוך-כדי-קירור כבר לא רק
     * "נדחית" - היא נשמרת כ-premove ורצה אוטומטית ברגע שהקירור פג (בלי
     * צורך לבקש שוב באופן ידני). זה מה שהטסט הזה בודק עכשיו.
     */
    @Test
    void cooldown_queuesAsPremove_thenFiresAutomaticallyAfterExpiry() {
        Board board = new BoardParser().parse("wR . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        MoveResult tooSoon = engine.requestMove(new Position(0, 1), new Position(0, 2));
        assertFalse(tooSoon.isAccepted(), "עדיין בקירור - לא מתבצע מיד");
        assertEquals(MoveReason.PREMOVE_QUEUED, tooSoon.reason(), "נשמר כ-premove, לא נדחה סתם");

        // Standard cooldown = 10 שניות, נספר מרגע ההגעה (t=1000) - אז עוד
        // 10 שניות מלאות עד שהקירור פג, ועוד 1000ms נוספות שזה ייקח למהלך

        engine.wait(10000);
        engine.wait(1000);

        assertNull(board.pieceAt(new Position(0, 1)));
        assertNotNull(board.pieceAt(new Position(0, 2)), "ה-premove רץ אוטומטית כשהקירור פג");
    }

    @Test
    void slidingPiece_canSelectDestinationBeyondAFriendlyBlocker() {
        Board board = new BoardParser().parse("wR wP .");
        GameEngine engine = engineFor(board);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(0, 2));

        assertTrue(result.isAccepted(), "מותר לבחור יעד מעבר לכלי ידידותי - הוא אולי יזוז בזמן");
    }

    @Test
    void stationaryFriendlyBlocker_stopsMovingPieceOneCellBefore_noAutoResume() {
        Board board = new BoardParser().parse("wR . wP . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 4));
        engine.wait(10000);

        assertNotNull(board.pieceAt(new Position(0, 1)), "הרוק נעצר תא אחד לפני הפאון");
        assertNotNull(board.pieceAt(new Position(0, 2)), "הפאון עדיין שם - לא נפגע");
        assertNull(board.pieceAt(new Position(0, 4)), "הרוק לא הגיע ליעד המקורי - אין חידוש אוטומטי");
    }

    @Test
    void stationaryFriendlyBlocker_thatClearsWellBeforeArrival_letsTheMovingPieceContinue() {
        Board board = new BoardParser().parse("wR . wK . .\n. . . . .");
        GameEngine engine = engineFor(board);

        engine.requestMove(new Position(0, 0), new Position(0, 4));
        engine.requestMove(new Position(0, 2), new Position(1, 2)); // המלך יורד לשורה אחרת - באמת יוצא מהמסלול (לא רק זז-קדימה-על-אותו-מסלול)

        // טיקים קטנים (כמו הלולאה האמיתית ב-GameWindow, כל 33ms) ולא קפיצה
        // גדולה אחת - ראו הערה בהודעה: קפיצת-זמן ענקית-אחת יכולה לגרום
        // ל-resolveDue לבדוק "האם החוסם עדיין שם" על-בסיס לוח שעדיין לא

        for (int i = 0; i < 20; i++) {
            engine.wait(500);
        }

        assertNull(board.pieceAt(new Position(0, 1)), "הרוק לא נעצר - הדרך התפנתה מוקדם מספיק");
        assertNotNull(board.pieceAt(new Position(0, 4)), "הרוק המשיך עד היעד המקורי");
        assertNotNull(board.pieceAt(new Position(1, 2)), "המלך יושב במיקומו החדש, מחוץ למסלול הרוק");
    }
}