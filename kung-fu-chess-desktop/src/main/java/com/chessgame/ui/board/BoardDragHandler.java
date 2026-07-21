package com.chessgame.ui.board;

import com.chessgame.input.Controller;
import com.chessgame.model.Position;

import java.awt.Point;

final class BoardDragHandler {
    private final Controller controller;

    private Position dragSource;
    private Point dragPixel;

    BoardDragHandler(Controller controller) {
        this.controller = controller;
    }

    Position dragSource() {
        return dragSource;
    }

    Point dragPixel() {
        return dragPixel;
    }

    boolean isDragging() {
        return dragSource != null;
    }

    /** "מרימים" את הכלי - אם יש כלי בתא, מתחילים לעקוב אחרי העכבר. */
    void beginDrag(Point relativePixel, int cellSizePx) {
        controller.setCellSizePx(cellSizePx);
        Position picked = controller.beginDrag(relativePixel.x, relativePixel.y);

        if (picked != null) {
            dragSource = picked;
            dragPixel = relativePixel;
        }
    }

    /** בזמן שהעכבר לחוץ וזז - רק מעדכנים את מיקום-התצוגה, לא נוגעים בלוגיקת המשחק. */
    void updateDrag(Point relativePixel) {
        if (dragSource == null) {
            return;
        }
        if (relativePixel != null) {
            dragPixel = relativePixel;
        }
    }

    /** "מניחים" את הכלי - מבקשים את המהלך האמיתי מהתא-המקורי אל תא-השחרור. */
    void endDrag(Point relativePixel) {
        if (dragSource == null) {
            return;
        }

        int x = (relativePixel != null) ? relativePixel.x : -1;
        int y = (relativePixel != null) ? relativePixel.y : -1;

        controller.endDrag(x, y);
        dragSource = null;
        dragPixel = null;
    }

    /** לחיצה כפולה על אותו כלי - קפיצה-במקום (כמו שכבר ממומש ב-Controller.jump). */
    void doubleClick(Point relativePixel, int cellSizePx) {
        if (relativePixel == null) {
            return;
        }

        controller.setCellSizePx(cellSizePx);
        controller.jump(relativePixel.x, relativePixel.y);

        dragSource = null;
        dragPixel = null;
    }
}
