package com.chessgame.model;

import java.util.HashMap;
import java.util.Map;


public final class Board {
    private final int width;
    private final int height;
    private final Map<Position, Piece> occupancy = new HashMap<>();

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** בדיקת "בתוך התחום" - בדיוק כמו שהמסמך דורש, זו אחריות של Board ולא של Position. */
    public boolean isInBounds(Position position) {
        return position.row() >= 0 && position.row() < height
                && position.col() >= 0 && position.col() < width;
    }

    /**
     * מוסיף כלי חדש למשבצת שלו (piece.cell()).
     * דוחה תפוסה כפולה - בדיוק כדרישת המסמך.
     */
    public void addPiece(Piece piece) {
        Position cell = piece.cell();
        if (occupancy.containsKey(cell)) {
            throw new IllegalStateException("Cell already occupied: " + cell);
        }
        occupancy.put(cell, piece);
    }

    /** מסיר כלי ממשבצת נתונה (למשל כלי שנלכד) - מנקה את התא שלו. */
    public void removePiece(Position cell) {
        occupancy.remove(cell);
    }

    /** שולף את הכלי בתא נתון, או null אם התא ריק. */
    public Piece pieceAt(Position cell) {
        return occupancy.get(cell);
    }

    /**
     * מזיז כלי ממקור ליעד, ומעדכן גם את המפה וגם את הכלי עצמו (piece.cell).
     * מניח שהוולידציה כבר קרתה - לא בודק שום חוק שחמט. אם יש כלי אויב
     * ביעד, הוא "נדרס" (זו בדיוק הלכידה, שכבר הוחלט עליה קודם ב-RealTimeArbiter).
     */
    public void movePiece(Position from, Position to) {
        Piece piece = occupancy.remove(from);
        if (piece == null) {
            throw new IllegalStateException("No piece at source cell: " + from);
        }
        occupancy.put(to, piece);
        piece.setCell(to);
    }
}
