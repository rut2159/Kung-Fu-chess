package com.chessgame.moves;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.MoveRuleRegistry;
import com.chessgame.rules.PieceMovementRule;

/**
 * MoveValidator / מאמת מהלכים
 *
 * תפקיד: ולידציה בסיסית + האצלה למרשם החוקים (חבילת rules).
 * public כי GameEngine (חבילת השורש) בונה ממנו מופע וקורא ל-isValid.
 */
public final class MoveValidator {
    private final MoveRuleRegistry ruleRegistry;

    public MoveValidator(MoveRuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    public boolean isValid(MoveContext ctx) {
        if (ctx.fromRow() == ctx.toRow() && ctx.fromCol() == ctx.toCol()) return false;
        if (ctx.movingPiece().isEmpty()) return false;

        PieceMovementRule rule = ruleRegistry.ruleFor(ctx.movingPiece().getType());
        return rule.isValid(ctx);
    }
}
