package com.chessgame.rules;

import com.chessgame.board.Board;
import com.chessgame.pieces.Piece;

/**
 * MoveContext / הקשר מהלך
 *
 * תפקיד: אובייקט "עטיפה" שמכיל את כל המידע שחוק תנועה עשוי להזדקק לו.
 * public כי נבנה ונקרא הן מחבילת moves (MoveValidator, MoveArrivalProcessor)
 * והן מחבילת השורש (GameEngine).
 */
public final class MoveContext {
    private final Board board;
    private final Piece movingPiece;
    private final int fromRow, fromCol, toRow, toCol;

    public MoveContext(Board board, Piece movingPiece, int fromRow, int fromCol, int toRow, int toCol) {
        this.board = board;
        this.movingPiece = movingPiece;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    public Board board() { return board; }
    public Piece movingPiece() { return movingPiece; }
    public int fromRow() { return fromRow; }
    public int fromCol() { return fromCol; }
    public int toRow() { return toRow; }
    public int toCol() { return toCol; }

    /** הפרש שורות מוחלט. */
    public int deltaRow() { return Math.abs(toRow - fromRow); }

    /** הפרש עמודות מוחלט. */
    public int deltaCol() { return Math.abs(toCol - fromCol); }
}
