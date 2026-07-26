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

    void beginDrag(Point relativePixel, int cellSizePx) {
        controller.setCellSizePx(cellSizePx);
        Position picked = controller.beginDrag(relativePixel.x, relativePixel.y);

        if (picked != null) {
            dragSource = picked;
            dragPixel = relativePixel;
        }
    }

    void updateDrag(Point relativePixel) {
        if (dragSource == null) {
            return;
        }
        if (relativePixel != null) {
            dragPixel = relativePixel;
        }
    }

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
