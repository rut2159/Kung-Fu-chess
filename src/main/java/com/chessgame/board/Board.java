package com.chessgame.board;

import com.chessgame.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Board / לוח
 *
 * תפקיד: אחסון גולמי של מטריצת Piece ופעולות בסיסיות עליה.
 * public כי בשימוש בחבילת rules (דרך MoveContext), moves, ובחבילת השורש
 * (GameEngine, CommandParser).
 */
public final class Board {
    private final List<Piece[]> grid = new ArrayList<>();

    public void addRow(Piece[] row) {
        grid.add(row);
    }

    public int getRowsCount() {
        return grid.size();
    }

    public int getColsCount() {
        return grid.isEmpty() ? 0 : grid.get(0).length;
    }

    public Piece getPiece(int row, int col) {
        return grid.get(row)[col];
    }

    public void setPiece(int row, int col, Piece piece) {
        grid.get(row)[col] = piece;
    }

    public void clearCell(int row, int col) {
        grid.get(row)[col] = Piece.EMPTY;
    }

    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < getRowsCount() && col >= 0 && col < getColsCount();
    }

    public void print() {
        for (Piece[] row : grid) {
            String[] tokens = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                tokens[i] = row[i].toDisplayString();
            }
            System.out.println(String.join(" ", tokens));
        }
    }
}
