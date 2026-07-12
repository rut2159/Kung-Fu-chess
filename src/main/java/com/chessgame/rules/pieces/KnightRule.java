package com.chessgame.rules.pieces;


import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashSet;
import java.util.Set;

/** KnightRule / חוק פרש - קפיצות L, מתעלם לגמרי מחוסמים (blockers). */
public final class KnightRule implements PieceRule {
    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        for (int[] offset : OFFSETS) {
            Position target = new Position(piece.cell().row() + offset[0], piece.cell().col() + offset[1]);
            if (!board.isInBounds(target)) continue;

            Piece occupant = board.pieceAt(target);
            if (occupant == null || occupant.isEnemyOf(piece)) {
                destinations.add(target);
            }
        }
        return destinations;
    }
}
