package com.chessgame.ui.board;

import com.chessgame.DesktopGameSession;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardControllerTest {

    private DesktopGameSession sessionFor(Board board) {
        return new DesktopGameSession(board);
    }

    @Test
    void panel_isNeverNull() {
        Board board = new BoardParser().parse("wK . .\n. . .\n. . .");
        DesktopGameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "Alice", "Bob");

        assertNotNull(controller.panel());
    }

    @Test
    void onGameStateChanged_doesNotThrow() {
        Board board = new BoardParser().parse("wK . .\n. . .\n. . .");
        DesktopGameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "Alice", "Bob");

        assertDoesNotThrow(() -> controller.onGameStateChanged(session.gameEngine));
    }

    @Test
    void registersAsListener_andReactsToAcceptedMove() {
        Board board = new BoardParser().parse("wR . .\n. . .\n. . .");
        DesktopGameSession session = sessionFor(board);
        new BoardController(session, "Alice", "Bob");

        assertDoesNotThrow(() -> {
            session.gameEngine.requestMove(new Position(0, 0), new Position(0, 1));
            session.gameEngine.wait(1100);
        });
    }

    /**
     * שחזור מלא של גרירה רגילה (press -> drag -> release) דרך ה-listener-ים
     * האמיתיים (לא reflection ישיר לתוך פונקציות פרטיות) - מוודא שהיא
     * באמת מבצעת מהלך על הלוח, לא רק שאין חריגה.
     */
    @Test
    void normalDragThroughRealListeners_actuallyExecutesTheMove() throws Exception {
        Board board = new BoardParser().parse("wR . . .");
        DesktopGameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "P2", "P1");

        BoardGeometry geometry = new BoardGeometry(400, 0, 0, 100);
        java.lang.reflect.Field geometryField = BoardController.class.getDeclaredField("geometry");
        geometryField.setAccessible(true);
        geometryField.set(controller, geometry);

        java.awt.event.MouseListener[] mouseListeners = controller.panel().getMouseListeners();
        java.awt.event.MouseMotionListener[] motionListeners = controller.panel().getMouseMotionListeners();

        java.awt.event.MouseEvent press = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 50, 50, 1, false);
        for (java.awt.event.MouseListener l : mouseListeners) l.mousePressed(press);

        java.awt.event.MouseEvent drag = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, 250, 50, 1, false);
        for (java.awt.event.MouseMotionListener l : motionListeners) l.mouseDragged(drag);

        java.awt.event.MouseEvent release = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 250, 50, 1, false);
        for (java.awt.event.MouseListener l : mouseListeners) l.mouseReleased(release);

        session.gameEngine.wait(3000); // 2 משבצות = 2000ms, קצת רזרבה

        assertNull(board.pieceAt(new Position(0, 0)), "הרוק כבר לא במקור");
        assertNotNull(board.pieceAt(new Position(0, 2)), "הרוק הגיע ליעד שנגרר אליו");
    }

    @Test
    void doubleClickOnAPiece_triggersJumpInPlace_notANormalMove() throws Exception {
        Board board = new BoardParser().parse("wR . . .");
        DesktopGameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "Alice", "Bob");

        // גיאומטריה קבועה וידועה, כדי לא להסתמך על תזמון componentResized של Swing.
        BoardGeometry geometry = new BoardGeometry(400, 0, 0, 100);
        java.lang.reflect.Field geometryField = BoardController.class.getDeclaredField("geometry");
        geometryField.setAccessible(true);
        geometryField.set(controller, geometry);

        Piece rook = board.pieceAt(new Position(0, 0));
        assertEquals(Piece.State.IDLE, rook.state());

        java.awt.event.MouseListener[] listeners = controller.panel().getMouseListeners();
        java.awt.event.MouseEvent doubleClickPress = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 50, 50, 2, false);

        for (java.awt.event.MouseListener listener : listeners) {
            listener.mousePressed(doubleClickPress);
        }

        assertEquals(Piece.State.AIRBORNE, rook.state(), "לחיצה כפולה על הכלי - קפיצה במקום, לא מהלך רגיל");
        assertNotNull(board.pieceAt(new Position(0, 0)), "הכלי עדיין באותו תא - זו קפיצה-במקום, לא הזזה");
    }

    /**
     * שחזור מדויק של הקריסה שדווחה: גרירת כלי ממש עד לפיקסלים הקיצוניים
     * של הלוח (פינה ימנית-תחתונה) גרמה בעבר ל-IllegalArgumentException
     * ("Patch exceeds destination bounds") בכל קריאה ל-render - כולל
     * מטיימר-האנימציה הרגיל, מה שהקפיא את הלוח לצמיתות. עכשיו המיקום
     * נצבט (clamp) כדי שהספרייט לעולם לא יחרוג מגבולות תמונת-הלוח.
     */
    @Test
    void draggingToTheExtremeEdgeOfTheBoard_neverThrows() throws Exception {
        Board board = new BoardParser().parse(
                "bR bN bB bQ bK bB bN bR\n" +
                        "bP bP bP bP bP bP bP bP\n" +
                        ". . . . . . . .\n" +
                        ". . . . . . . .\n" +
                        ". . . . . . . .\n" +
                        ". . . . . . . .\n" +
                        "wP wP wP wP wP wP wP wP\n" +
                        "wR wN wB wQ wK wB wN wR"
        );
        DesktopGameSession session = sessionFor(board);
        BoardController controller = new BoardController(session, "P2", "P1");

        BoardGeometry geometry = new BoardGeometry(800, 0, 0, 100);
        java.lang.reflect.Field geometryField = BoardController.class.getDeclaredField("geometry");
        geometryField.setAccessible(true);
        geometryField.set(controller, geometry);

        java.awt.event.MouseListener[] mouseListeners = controller.panel().getMouseListeners();
        java.awt.event.MouseMotionListener[] motionListeners = controller.panel().getMouseMotionListeners();

        java.awt.event.MouseEvent press = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 750, 750, 1, false);
        for (java.awt.event.MouseListener l : mouseListeners) l.mousePressed(press); // תופסים את הרוק בפינה h1

        int[][] extremePoints = {{799, 799}, {798, 750}, {750, 798}, {0, 0}, {1, 1}};
        for (int[] p : extremePoints) {
            java.awt.event.MouseEvent drag = new java.awt.event.MouseEvent(
                    controller.panel(), java.awt.event.MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, p[0], p[1], 1, false);
            assertDoesNotThrow(() -> {
                for (java.awt.event.MouseMotionListener l : motionListeners) l.mouseDragged(drag);
            });
        }

        java.awt.event.MouseEvent release = new java.awt.event.MouseEvent(
                controller.panel(), java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 750, 750, 1, false);
        assertDoesNotThrow(() -> {
            for (java.awt.event.MouseListener l : mouseListeners) l.mouseReleased(release);
        });

        assertDoesNotThrow(() -> session.gameEngine.wait(500), "גם ריצת-הטיימר-הרגילה לא אמורה להיכשל");
    }
}
