package com.chessgame.rules.standard;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * QueenMovementRule / חוק תנועת מלכה
 *
 * תפקיד: מרכיבה (composition) את RookMovementRule ו-BishopMovementRule
 * במקום לשכפל את הלוגיקה שלהן. package-private (ר' הסבר ב-KingMovementRule).
 */
class QueenMovementRule implements PieceMovementRule {
    private final RookMovementRule asRook = new RookMovementRule();
    private final BishopMovementRule asBishop = new BishopMovementRule();

    @Override
    public boolean isValid(MoveContext ctx) {
        return asRook.isValid(ctx) || asBishop.isValid(ctx);
    }
}
