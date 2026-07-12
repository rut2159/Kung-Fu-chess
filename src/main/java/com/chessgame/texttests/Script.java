package com.chessgame.texttests;

import java.util.Collections;
import java.util.List;

/**
 * Script / סקריפט
 *
 * תפקיד: מבנה-נתונים "פסיבי" בלבד - תוצאת-הפרסינג של קובץ-סקריפט.
 * מכיל את טקסט-הלוח ההתחלתי (שיועבר ל-BoardParser), ורשימת-פקודות
 * מסודרת (click/wait/print-board-עם-הציפייה-הצמודה-אליו).
 *
 * שים לב: Script לא "יודע" איך להריץ את עצמו - זו אחריות של
 * ScriptRunner. Script רק מחזיק מידע, בדיוק כמו MoveResult/
 * MoveValidation שבנינו קודם.
 */
public final class Script {
    private final String boardText;
    private final List<Command> commands;

    public Script(String boardText, List<Command> commands) {
        this.boardText = boardText;
        this.commands = Collections.unmodifiableList(commands);
    }

    public String boardText() {
        return boardText;
    }

    public List<Command> commands() {
        return commands;
    }

    /** Command / פקודה - סימון-בלבד (marker), בלי שום מתודה משותפת. */
    public interface Command {
    }

    public static final class ClickCommand implements Command {
        public final int x;
        public final int y;

        public ClickCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * JumpCommand / פקודת-קפיצה
     *
     * תפקיד: תוספת שלנו ל-DSL, מעבר ל-4 הפקודות הרשמיות של המסמך
     * (Board/click/wait/print board) - בדיוק כמו ש-Controller.jump()
     * ו-App/CommandDispatcher כבר תומכים ב-"jump X Y" כפקודת-הרצה
     * אמיתית. מתועד כאן במפורש כתוספת, לא כחלק מהדרישה הרשמית.
     */
    public static final class JumpCommand implements Command {
        public final int x;
        public final int y;

        public JumpCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class WaitCommand implements Command {
        public final int milliseconds;

        public WaitCommand(int milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    public static final class PrintBoardCommand implements Command {
        public final List<String> expectedRows;

        public PrintBoardCommand(List<String> expectedRows) {
            this.expectedRows = Collections.unmodifiableList(expectedRows);
        }
    }
}
