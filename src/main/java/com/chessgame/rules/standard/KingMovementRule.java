package com.chessgame.rules.standard;

import com.chessgame.rules.MoveContext;
import com.chessgame.rules.PieceMovementRule;

/**
 * KingMovementRule / חוק תנועת מלך
 *
 * תפקיד: תא אחד לכל כיוון. package-private - נוצרת ונרשמת רק בתוך
 * StandardChessRuleSet (אותה תת-חבילה); שום קוד מחוץ ל-rules.standard
 * לא צריך לדעת שהמחלקה הזו קיימת בכלל - הוא מקבל אותה בעקיפין דרך
 * MoveRuleRegistry.ruleFor(...) שמחזיר PieceMovementRule (הממשק הpublic
 * שיושב בחבילת rules "האמא").
 */
class KingMovementRule implements PieceMovementRule {
    @Override
    public boolean isValid(MoveContext ctx) {
        return ctx.deltaRow() <= 1 && ctx.deltaCol() <= 1;
    }
}
