package com.chessgame.rules.standard;

import com.chessgame.rules.MoveRuleRegistry;

/**
 * StandardChessRuleSet / ערכת חוקי שחמט רגיל
 *
 * תפקיד: המקום היחיד בפרויקט שבו כתוב "אלו הכללים של שחמט רגיל".
 * public כי GameEngine (חבילת השורש) קורא ל-build() ישירות. ממוקמת
 * ב-rules.standard יחד עם כל מחלקות ה-*MovementRule הקונקרטיות שהיא
 * מרכיבה - זה בדיוק "החבילה של ruleset אחד ספציפי". בעתיד, ruleset
 * מותאם אישית (למשל "שחמט קונג-פו בסגנון שלומי") יקבל תת-חבילה
 * מקבילה, כגון com.chessgame.rules.kungfu, עם אותו מבנה בדיוק - בלי
 * לגעת בכלל ב-rules.standard או ב-rules "האמא".
 */
public final class StandardChessRuleSet {
    private StandardChessRuleSet() {
        // מחלקת הרכבה סטטית בלבד.
    }

    public static MoveRuleRegistry build() {
        MoveRuleRegistry registry = new MoveRuleRegistry();

        registry.register('K', new KingMovementRule());
        registry.register('Q', new QueenMovementRule());
        registry.register('R', new RookMovementRule());
        registry.register('B', new BishopMovementRule());
        registry.register('N', new KnightMovementRule());
        registry.register('P', new PawnMovementRule());

        registry.registerGameEndingCapture('K');

        return registry;
    }
}
