package com.chessgame.rules.pieces;


import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Set;

final class SlidingMovement {
    private SlidingMovement() {
        // מחלקת עזר סטטית בלבד.
    }

    static void addSlidingDirection(Board board, Piece piece, int dRow, int dCol, Set<Position> destinations) {
        int row = piece.cell().row() + dRow;
        int col = piece.cell().col() + dCol;

        while (board.isInBounds(new Position(row, col))) {
            Position current = new Position(row, col);
            Piece occupant = board.pieceAt(current);

            if (occupant == null) {
                destinations.add(current);
            } else if (occupant.isEnemyOf(piece)) {
                destinations.add(current); // אפשר ללכוד - אבל לא ממשיכים מעבר
                break;
            } else {
                break; // כלי ידידותי - חוסם לגמרי, לא כולל את עצמו
            }

            row += dRow;
            col += dCol;
        }
    }
}
