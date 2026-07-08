package com.chessgame.rules.standard;

import com.chessgame.board.BoardPath;
import com.chessgame.rules.MoveContext;

/**
 * PathUtil / עזר-נתיב
 *
 * תפקיד: "מסלול פנוי" עבור חוקי תנועה החלקה (רוק, רץ, מלכה).
 * שימו לב: המחלקה נשארת package-private (בלי public) - היא נחוצה רק
 * בתוך rules.standard עצמה (על ידי RookMovementRule/BishopMovementRule/
 * QueenMovementRule), ואין שום סיבה שחבילה אחרת - אפילו rules
 * "האמא" - תדע שהיא קיימת בכלל. זו בדיוק הנקודה של תת-החבילה הזו:
 * "איך מיושמים חוקי שחמט רגיל" זה פרט מימוש שכולו מוסתר מאחורי
 * MoveRuleRegistry.
 */
final class PathUtil {
    private PathUtil() {
        // מחלקת עזר סטטית בלבד.
    }

    /** האם כל התאים בין fromRow/fromCol ל-toRow/toCol (לא כולל קצוות) ריקים. */
    static boolean isPathClear(MoveContext ctx) {
        for (int[] cell : BoardPath.strictlyBetween(ctx.fromRow(), ctx.fromCol(), ctx.toRow(), ctx.toCol())) {
            if (!ctx.board().getPiece(cell[0], cell[1]).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
