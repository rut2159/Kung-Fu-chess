package com.chessgame.rules.pieces;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Set;

final class SlidingMovement {
    private SlidingMovement() {
    }

    static void addSlidingDirection(Board board, Piece piece, int dRow, int dCol, Set<Position> destinations) {
        int row = piece.cell().row() + dRow;
        int col = piece.cell().col() + dCol;

        while (board.isInBounds(new Position(row, col))) {
            Position current = new Position(row, col);
            Piece occupant = board.pieceAt(current);

            // אי אפשר לבחור לנחות ממש על כלי ידידותי - אבל כן אפשר לבחור
            // יעד מעבר אליו (הוא אולי יזוז בזמן) - ולכן לא עוצרים את הסריקה,
            // רק מדלגים על הוספת התא הזה עצמו.
            boolean blockedByFriendly = occupant != null && occupant.isSameColorAs(piece);
            if (!blockedByFriendly) {
                destinations.add(current);
            }

            row += dRow;
            col += dCol;
        }
    }
}
