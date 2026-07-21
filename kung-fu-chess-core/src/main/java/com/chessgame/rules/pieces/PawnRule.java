package com.chessgame.rules.pieces;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashSet;
import java.util.Set;

public final class PawnRule implements PieceRule {
    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        int direction = (piece.color() == Piece.Color.WHITE) ? -1 : 1;
        int row = piece.cell().row();
        int col = piece.cell().col();

        Position oneStep = new Position(row + direction, col);
        boolean oneStepClear = board.isInBounds(oneStep) && board.pieceAt(oneStep) == null;
        if (oneStepClear) {
            destinations.add(oneStep);

            int startRow = (piece.color() == Piece.Color.WHITE) ? board.height() - 2 : 1;
            if (row == startRow) {
                Position twoStep = new Position(row + 2 * direction, col);
                if (board.isInBounds(twoStep) && board.pieceAt(twoStep) == null) {
                    destinations.add(twoStep);
                }
            }
        }

        for (int dCol : new int[]{-1, 1}) {
            Position diagonal = new Position(row + direction, col + dCol);
            if (!board.isInBounds(diagonal)) continue;
            Piece occupant = board.pieceAt(diagonal);
            if (occupant != null && occupant.isEnemyOf(piece)) {
                destinations.add(diagonal);
            }
        }

        return destinations;
    }
}
