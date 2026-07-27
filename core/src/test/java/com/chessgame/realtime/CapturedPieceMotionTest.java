package com.chessgame.realtime;

import com.chessgame.GameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * כשכלי נהרג, התנועה הפעילה שלו חייבת למות איתו.
 *
 * כלי שיצא לתנועה נשאר רשום ב-occupancy במשבצת המוצא שלו עד שהוא מגיע
 * ליעד. לכן כל בדיקה מהצורה "מי יושב על התא הזה" לא מבחינה בין כלי שבאמת
 * עומד שם לבין כלי שכבר המריא ממנו. הרגרסיות כאן מכסות את שתי הדרכים שבהן
 * זה השתבש: קריסה מוחלטת של הטיק, וטלפורטציה שקטה של הכלי הלא-נכון.
 */
class CapturedPieceMotionTest {
    private static GameSession session(String text) {
        Board board = new BoardParser().parse(text);
        return new GameSession(board);
    }

    /**
     * חוסם שיצא לדרך לפני שהתוקף הגיע אליו כבר לא נמצא שם - אסור להרוג
     * אותו באוויר. לפני התיקון: הוא נהרג, התנועה שלו נשארה יתומה, ובזמן
     * ההגעה שלה movePiece מצא משבצת מוצא ריקה וזרק IllegalStateException
     * שהפיל את הטיק כולו באמצע.
     */
    @Test
    void blockerThatAlreadyTookOff_isNotCapturedInMidair() {
        GameSession game = session("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  bR .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  wR .  .  .  .  .  .
                """);

        assertTrue(game.gameEngine.requestMove(new Position(7, 1), new Position(0, 1)).isAccepted());
        game.gameEngine.wait(1000);

        // השחור בורח הצידה - הוא יהיה עדיין באוויר כשהלבן יחצה את (5,1).
        assertTrue(game.gameEngine.requestMove(new Position(5, 1), new Position(5, 5)).isAccepted());

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                game.gameEngine.wait(1000);
            }
        }, "advancing time must never throw - a thrown tick freezes the game clock");

        Piece escaped = game.board.pieceAt(new Position(5, 5));
        assertNotNull(escaped, "the black rook escaped in time and must be alive at its destination");
        assertEquals(Piece.Color.BLACK, escaped.color());
        assertNull(game.board.pieceAt(new Position(5, 1)), "it must not still be sitting at its origin");
    }

    /**
     * אכילה של כלי בדיוק על המשבצת שהוא ממריא ממנה. לפני התיקון: התנועה
     * של הקורבן שרדה את מותו, וכשהיא "הגיעה" היא הרימה את מי שיושב במשבצת
     * המוצא - כלומר את האוכל עצמו - והעיפה אותו ליעד של הקורבן.
     */
    @Test
    void capturingAPieceOnItsTakeoffSquare_doesNotDragTheCapturerAway() {
        GameSession game = session("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                bR .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                wR .  .  .  .  .  .  .
                """);

        Piece whiteRook = game.board.pieceAt(new Position(7, 0));

        // השחור יוצא למסע ארוך; הלבן אוכל אותו על משבצת ההמראה בזמן שהוא באוויר.
        assertTrue(game.gameEngine.requestMove(new Position(5, 0), new Position(5, 7)).isAccepted());
        game.gameEngine.wait(500);
        assertTrue(game.gameEngine.requestMove(new Position(7, 0), new Position(5, 0)).isAccepted());

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 12; i++) {
                game.gameEngine.wait(1000);
            }
        });

        assertSame(whiteRook, game.board.pieceAt(new Position(5, 0)),
                "the capturer must stay on the square it captured, not follow its victim's route");
        assertNull(game.board.pieceAt(new Position(5, 7)),
                "the dead rook's motion must not complete after it was captured");
        assertEquals(1, game.board.allPieces().size());
    }

    /**
     * הגרסה הידידותית של אותה מלכודת: חוסם ידידותי שכבר עזב לא אמור לעצור
     * כלי אחר. לפני התיקון הכלי הנע נעצר תא לפני חוסם שכבר לא היה שם.
     */
    @Test
    void friendlyBlockerThatAlreadyTookOff_doesNotStopTheMover() {
        GameSession game = session("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  wR .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  wR .  .  .  .  .  .
                """);

        // הצריח הידידותי ב-(5,1) יוצא לדרך ראשון, ורק אחר כך זה שמאחוריו.
        assertTrue(game.gameEngine.requestMove(new Position(5, 1), new Position(5, 6)).isAccepted());
        assertTrue(game.gameEngine.requestMove(new Position(7, 1), new Position(2, 1)).isAccepted());

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 12; i++) {
                game.gameEngine.wait(1000);
            }
        });

        assertNotNull(game.board.pieceAt(new Position(2, 1)),
                "the rear rook should reach its destination - the blocker had already left");
        assertNotNull(game.board.pieceAt(new Position(5, 6)));
    }

    /**
     * המסלול חייב להיות ישר או אלכסוני. עד לתיקון, מסלול אחר גרם ללולאת
     * while אינסופית - קיפאון מוחלט ובלתי הפיך של השרת. גם אם היום שום
     * קריאה לא מגיעה לשם עם קלט כזה (הפרש מסונן במפורש), עדיף להתפוצץ
     * ברעש מאשר להיתקע בשקט.
     */
    @Test
    void knightMove_terminates() {
        GameSession game = session("""
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                wN .  .  .  .  .  .  .
                """);

        assertTimely(() -> {
            game.gameEngine.requestMove(new Position(7, 0), new Position(5, 1));
            for (int i = 0; i < 5; i++) {
                game.gameEngine.wait(1000);
            }
        });

        assertNotNull(game.board.pieceAt(new Position(5, 1)));
    }

    /** מריץ בתוך thread נפרד כדי שלולאה אינסופית תיכשל כטסט ולא תתקע את כל ה-build. */
    private static void assertTimely(Runnable body) {
        Throwable[] thrown = new Throwable[1];
        Thread thread = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                thrown[0] = t;
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            throw new AssertionError("did not terminate within 10s - suspected infinite loop");
        }
        if (thrown[0] != null) {
            throw new AssertionError("threw " + thrown[0], thrown[0]);
        }
    }
}
