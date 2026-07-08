package com.chessgame;

import com.chessgame.board.Board;

import java.util.Optional;

/**
 * CommandParser / מפרסר פקודות
 *
 * תפקיד: הופך "cmd X Y" (בפיקסלים) לתא בלוח. נשאר package-private -
 * בשימוש רק בתוך GameEngine (אותה חבילה). צריך import ל-Board כי
 * הוא מוגדר בחבילה אחרת (board).
 */
final class CommandParser {
    private static final int CELL_SIZE_PX = 100;

    private CommandParser() {
        // מחלקת עזר סטטית בלבד.
    }

    static Optional<Cell> parseCellCommand(String line, Board board) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) return Optional.empty();

        int x, y;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        if (x < 0 || y < 0) return Optional.empty();

        int col = x / CELL_SIZE_PX;
        int row = y / CELL_SIZE_PX;

        if (!board.isValidCell(row, col)) return Optional.empty();

        return Optional.of(new Cell(row, col));
    }
}
