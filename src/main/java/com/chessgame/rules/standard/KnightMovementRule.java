package com.chessgame.rules.standard;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * KnightMovementRule / חוק תנועת פרש
 *
 * תפקיד: צורת L, בלי בדיקת מסלול (פרש "קופץ" מעל כלים). package-private
 * (ר' הסבר ב-KingMovementRule).
 */
class KnightMovementRule implements PieceMovementRule {
    @Override
    public boolean isValid(MoveContext ctx) {
        return (ctx.deltaRow() == 1 && ctx.deltaCol() == 2)
                || (ctx.deltaRow() == 2 && ctx.deltaCol() == 1);
    }
}
