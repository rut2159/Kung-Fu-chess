package com.chessgame.rules.pieces;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.HashSet;
import java.util.Set;

/** BishopRule / חוק רץ - סליידינג אלכסוני עד חסימה. */
public final class BishopRule implements PieceRule {
    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        SlidingMovement.addSlidingDirection(board, piece, -1, -1, destinations);
        SlidingMovement.addSlidingDirection(board, piece, -1, 1, destinations);
        SlidingMovement.addSlidingDirection(board, piece, 1, -1, destinations);
        SlidingMovement.addSlidingDirection(board, piece, 1, 1, destinations);
        return destinations;
    }
}
