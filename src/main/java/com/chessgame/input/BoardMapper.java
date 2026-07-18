package com.chessgame.input;

import com.chessgame.model.Board;
import com.chessgame.model.Position;

public final class BoardMapper {
    private static final int DEFAULT_CELL_SIZE_PX = 100;

    private final Board board;
    private int cellSizePx = DEFAULT_CELL_SIZE_PX;

    public BoardMapper(Board board) {
        this.board = board;
    }

    public void setCellSizePx(int cellSizePx) {
        if (cellSizePx <= 0) {
            throw new IllegalArgumentException("cellSizePx must be positive: " + cellSizePx);
        }
        this.cellSizePx = cellSizePx;
    }

    public int cellSizePx() {
        return cellSizePx;
    }

    public Position pixelToCell(int x, int y) {
        if (x < 0 || y < 0) return null;
        Position candidate = new Position(y / cellSizePx, x / cellSizePx);
        return board.isInBounds(candidate) ? candidate : null;
    }
}
