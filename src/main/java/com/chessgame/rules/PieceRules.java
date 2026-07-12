package com.chessgame.rules;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.rules.pieces.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * PieceRules / מרשם חוקים
 *
 * תפקיד: ממפה כל Piece.Kind לחוק-התנועה שלו, ומספקת נקודת-כניסה
 * אחת ("legalDestinations") שכל שאר המערכת (בעיקר RuleEngine) קוראת
 * לה - בלי לדעת בכלל שיש מאחוריה 6 מחלקות נפרדות.
 */
public final class PieceRules {
    private final Map<Piece.Kind, PieceRule> rules = new EnumMap<>(Piece.Kind.class);

    public PieceRules() {
        rules.put(Piece.Kind.ROOK, new RookRule());
        rules.put(Piece.Kind.BISHOP, new BishopRule());
        rules.put(Piece.Kind.QUEEN, new QueenRule());
        rules.put(Piece.Kind.KNIGHT, new KnightRule());
        rules.put(Piece.Kind.KING, new KingRule());
        rules.put(Piece.Kind.PAWN, new PawnRule());
    }

    public Set<Position> legalDestinations(Board board, Piece piece) {
        return rules.get(piece.kind()).legalDestinations(board, piece);
    }
}
