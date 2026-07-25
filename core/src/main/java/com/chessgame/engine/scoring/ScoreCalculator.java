package com.chessgame.engine.scoring;

import com.chessgame.model.Piece;

import java.util.List;

public final class ScoreCalculator {
    private ScoreCalculator() {}

    public static int score(List<Piece> roster, Piece.Color scoringColor) {
        int total = 0;
        for (Piece piece : roster) {
            if (piece.color() != scoringColor && piece.state() == Piece.State.CAPTURED) {
                total += pieceValue(piece.kind());
            }
        }
        return total;
    }

    private static int pieceValue(Piece.Kind kind) {
        switch (kind) {
            case PAWN: return 1;
            case KNIGHT: return 3;
            case BISHOP: return 3;
            case ROOK: return 5;
            case QUEEN: return 9;
            case KING: return 0;
            default: return 0;
        }
    }
}
