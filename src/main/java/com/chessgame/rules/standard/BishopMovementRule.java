package com.chessgame.rules.standard;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * BishopMovementRule / חוק תנועת רץ
 *
 * תפקיד: תנועה אלכסונית עם מסלול פנוי. package-private (ר' הסבר ב-KingMovementRule).
 */
class BishopMovementRule implements PieceMovementRule {
    @Override
    public boolean isValid(MoveContext ctx) {
        boolean diagonal = ctx.deltaRow() == ctx.deltaCol();
        return diagonal && PathUtil.isPathClear(ctx);
    }
}
