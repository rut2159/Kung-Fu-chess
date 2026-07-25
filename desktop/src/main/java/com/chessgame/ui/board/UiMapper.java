package com.chessgame.ui.board;

import com.chessgame.model.Position;
import java.awt.Point;

public final class UiMapper {
    private static final int DEFAULT_CELL_SIZE = 100;

    private int cellSize = DEFAULT_CELL_SIZE;

    public UiMapper() {
    }

    public void setCellSize(int cellSize) {
        if (cellSize <= 0) {
            throw new IllegalArgumentException("cellSize must be positive: " + cellSize);
        }
        this.cellSize = cellSize;
    }

    public int getCellSize() {
        return cellSize;
    }

    public Point cellToPixel(Position pos) {
        return cellToPixel((double) pos.row(), (double) pos.col());
    }

    public Point cellToPixel(double row, double col) {
        int x = (int) Math.round(col * cellSize);
        int y = (int) Math.round(row * cellSize);
        return new Point(x, y);
    }

    public Position pixelToCell(int x, int y) {
        if (x < 0 || y < 0) return null;
        return new Position(y / cellSize, x / cellSize);
    }
}
