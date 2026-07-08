package com.chessgame.rules;

import com.chessgame.pieces.Piece;

/**
 * PieceMovementRule / ממשק חוק תנועה
 *
 * תפקיד: ה"חוזה" שכל התנהגות-של-כלי מיישמת. public כי חבילת moves
 * (MoveArrivalProcessor) מצהירה משתנים מהטיפוס הזה.
 */
public interface PieceMovementRule {

    /** האם המהלך המתואר ב-ctx חוקי עבור סוג הכלי הזה. */
    boolean isValid(MoveContext ctx);

    /**
     * מה קורה לכלי כשהוא מגיע בהצלחה ליעד שלו.
     * ברירת המחדל: הכלי נשאר אותו כלי בדיוק.
     */
    default Piece resolveArrival(MoveContext ctx) {
        return ctx.movingPiece();
    }
}
