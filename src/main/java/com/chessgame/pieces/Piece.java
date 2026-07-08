package com.chessgame.pieces;

import java.util.Optional;

/**
 * Piece / כלי
 *
 * תפקיד: מייצגת "זהות" של כלי בלבד - צבע וסוג. שום לוגיקת תנועה,
 * שום ולידציה של "מהלך חוקי" לא נמצאת כאן - זו אחריות של PieceMovementRule
 * (בחבילת rules).
 *
 * שימו לב: המחלקה public כי היא בשימוש בכל שאר החבילות (board, rules,
 * moves, וחבילת השורש) - זה "כרטיס הזהות" המשותף לכל הפרויקט.
 */
public final class Piece {

    /** כלי-ריק (Null Object) - מחליף את ה-"." שהיה קיים כמחרוזת גולמית. */
    public static final Piece EMPTY = new Piece('\0', '\0');

    // הפורמט המדויק של טוקן קלט תקין: נקודה (ריק) או אות-צבע + אות-סוג.
    // שימו לב: [A-Z] כללי ולא [KQRBNP] ספציפי - זו בדיוק הפתיחות
    // הנדרשת לתמיכה עתידית בכלים מותאמים אישית.
    private static final String TOKEN_PATTERN = "^(\\.|[wb][A-Z])$";

    private final char color; // 'w' / 'b', או '\0' אם ריק
    private final char type;  // 'K','Q','R'... (או כל אות אחרת בעתיד), או '\0' אם ריק

    private Piece(char color, char type) {
        this.color = color;
        this.type = type;
    }

    /** יוצר כלי חדש לפי צבע וסוג. */
    public static Piece of(char color, char type) {
        return new Piece(color, type);
    }

    /**
     * מנתח טוקן קלט גולמי (למשל "wP" או ".") לאובייקט Piece.
     * מחזיר Optional.empty() אם הטוקן לא תקין מבנית.
     */
    public static Optional<Piece> parse(String token) {
        if (!token.matches(TOKEN_PATTERN)) {
            return Optional.empty();
        }
        if (token.equals(".")) {
            return Optional.of(EMPTY);
        }
        return Optional.of(new Piece(token.charAt(0), token.charAt(1)));
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public char getColor() {
        return color;
    }

    public char getType() {
        return type;
    }

    /** האם שני הכלים מאותו צבע (שניהם לא ריקים). */
    public boolean isFriendlyTo(Piece other) {
        if (this.isEmpty() || other.isEmpty()) return false;
        return this.color == other.color;
    }

    /** האם שני הכלים מצבעים שונים (שניהם לא ריקים). */
    public boolean isEnemyOf(Piece other) {
        if (this.isEmpty() || other.isEmpty()) return false;
        return this.color != other.color;
    }

    /** הייצוג הטקסטואלי להדפסה (בדיוק כמו הפורמט המקורי: "wP" או "."). */
    public String toDisplayString() {
        return isEmpty() ? "." : "" + color + type;
    }
}
