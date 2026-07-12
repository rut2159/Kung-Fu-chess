package com.chessgame.rules.pieces;


import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashSet;
import java.util.Set;

/** QueenRule / חוק מלכה - Rook movement plus bishop movement, בהרכבה (composition). */
public final class QueenRule implements PieceRule {
    private final RookRule asRook = new RookRule();
    private final BishopRule asBishop = new BishopRule();

    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>(asRook.legalDestinations(board, piece));
        destinations.addAll(asBishop.legalDestinations(board, piece));
        return destinations;
    }
}
