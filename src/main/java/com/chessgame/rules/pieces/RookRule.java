package com.chessgame.rules.pieces;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.pieces.SlidingMovement;

import java.util.HashSet;
import java.util.Set;

public final class RookRule implements PieceRule {
    @Override
    public Set<Position> legalDestinations(Board board, Piece piece) {

        Set<Position> destinations = new HashSet<>();

        SlidingMovement.addSlidingDirection(board, piece, -1, 0, destinations); // למעלה
        SlidingMovement.addSlidingDirection(board, piece, 1, 0, destinations);  // למטה
        SlidingMovement.addSlidingDirection(board, piece, 0, -1, destinations); // שמאלה
        SlidingMovement.addSlidingDirection(board, piece, 0, 1, destinations);  // ימינה
        return destinations;
    }
}
