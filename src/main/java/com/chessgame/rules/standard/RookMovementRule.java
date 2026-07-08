package com.chessgame.rules.standard;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * RookMovementRule / חוק תנועת צריח
 *
 * תפקיד: תנועה בקו ישר עם מסלול פנוי. package-private (ר' הסבר ב-KingMovementRule).
 */
class RookMovementRule implements PieceMovementRule {
    @Override
    public boolean isValid(MoveContext ctx) {
        boolean straightLine = ctx.deltaRow() == 0 || ctx.deltaCol() == 0;
        return straightLine && PathUtil.isPathClear(ctx);
    }
}
