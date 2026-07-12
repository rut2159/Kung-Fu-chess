package com.chessgame.input;

import com.chessgame.model.Board;
import com.chessgame.model.Position;

public final class BoardMapper {
    private static final int CELL_SIZE_PX = 100;
    private final Board board;

    public BoardMapper(Board board) {
        this.board = board;
    }

    /** ממיר פיקסלים לתא-לוח, או null אם מחוץ לגבולות (כולל שליליים). */
    public Position pixelToCell(int x, int y) {
        if (x < 0 || y < 0) return null;
        Position candidate = new Position(y / CELL_SIZE_PX, x / CELL_SIZE_PX);
        return board.isInBounds(candidate) ? candidate : null;
    }
}
