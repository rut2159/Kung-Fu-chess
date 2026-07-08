package com.chessgame.board;

import com.chessgame.pieces.Piece;

/**
 * BoardParser / מפרסר לוח
 *
 * תפקיד: הופך שורת טקסט גולמית למערך Piece, ומוודא שכל השורות באותו
 * רוחב. public כי GameEngine (בחבילת השורש) משתמש בו וקולט את החריגה
 * שלו - ולכן גם BoardParseException חייבת להיות public.
 */
public final class BoardParser {
    private int expectedCols = -1;

    /** BoardParseException - חריגה שמסמנת בעיית קלט בפרסינג הלוח. */
    public static final class BoardParseException extends RuntimeException {
        public BoardParseException(String errorCode) {
            super(errorCode);
        }
    }

    public Piece[] parseRow(String line) {
        String[] tokens = line.split("\\s+");
        Piece[] row = new Piece[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            row[i] = Piece.parse(tokens[i])
                    .orElseThrow(() -> new BoardParseException("UNKNOWN_TOKEN"));
        }

        if (expectedCols == -1) {
            expectedCols = row.length;
        } else if (row.length != expectedCols) {
            throw new BoardParseException("ROW_WIDTH_MISMATCH");
        }

        return row;
    }
}
