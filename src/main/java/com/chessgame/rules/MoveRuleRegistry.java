package com.chessgame.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MoveRuleRegistry / מרשם חוקים
 *
 * תפקיד: מיפוי תו (סוג כלי) לאובייקט-חוק, ומיפוי "אילו לכידות מסיימות
 * את המשחק". public כי בשימוש בחבילת moves (MoveValidator,
 * MoveArrivalProcessor) ובחבילת השורש (GameEngine).
 */
public final class MoveRuleRegistry {
    private final Map<Character, PieceMovementRule> rules = new HashMap<>();
    private final Set<Character> gameEndingCaptureTypes = new HashSet<>();

    /** רושם חוק תנועה עבור סוג כלי נתון. */
    public void register(char pieceType, PieceMovementRule rule) {
        rules.put(pieceType, rule);
    }

    /** מסמן שסוג כלי מסוים, אם נלכד, מסיים את המשחק. */
    public void registerGameEndingCapture(char pieceType) {
        gameEndingCaptureTypes.add(pieceType);
    }

    /** מחזיר את חוק התנועה הרשום לסוג הכלי, או שגיאה ברורה אם לא נרשם אף חוק. */
    public PieceMovementRule ruleFor(char pieceType) {
        PieceMovementRule rule = rules.get(pieceType);
        if (rule == null) {
            throw new IllegalArgumentException("No movement rule registered for piece type: " + pieceType);
        }
        return rule;
    }

    /** האם לכידת כלי מהסוג הנתון אמורה לסיים את המשחק. */
    public boolean endsGameWhenCaptured(char pieceType) {
        return gameEndingCaptureTypes.contains(pieceType);
    }
}
