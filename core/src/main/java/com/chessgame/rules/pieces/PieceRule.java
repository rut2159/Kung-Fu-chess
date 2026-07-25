package com.chessgame.rules.pieces;


import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.Set;


public interface PieceRule {
    Set<Position> legalDestinations(Board board, Piece piece);
}
