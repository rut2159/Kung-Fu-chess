package com.chessgame.rules;


import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Set;

public final class RuleEngine {
    private final Board board;
    private final PieceRules pieceRules;

    public RuleEngine(Board board, PieceRules pieceRules) {
        this.board = board;
        this.pieceRules = pieceRules;
    }

    public MoveValidation validateMove(Position source, Position destination) {
        if (!board.isInBounds(source) || !board.isInBounds(destination)) {
            return MoveValidation.invalid(MoveReason.OUTSIDE_BOARD);
        }

        Piece movingPiece = board.pieceAt(source);
        if (movingPiece == null) {
            return MoveValidation.invalid(MoveReason.EMPTY_SOURCE);
        }

        if (movingPiece.kind() != Piece.Kind.KNIGHT) {
            Piece destinationOccupant = board.pieceAt(destination);
            if (destinationOccupant != null && destinationOccupant.isSameColorAs(movingPiece)) {
                return MoveValidation.invalid(MoveReason.FRIENDLY_DESTINATION);
            }
        }

        Set<Position> legalDestinations = pieceRules.legalDestinations(board, movingPiece);
        if (!legalDestinations.contains(destination)) {
            return MoveValidation.invalid(MoveReason.ILLEGAL_PIECE_MOVE);
        }

        return MoveValidation.valid();
    }
}
