package com.chessgame.realtime;

/**
 * קונפיגורציית מהירות למשחק - כמה זמן לוקח לכלי לעבור משבצת אחת,
 * וכמה זמן נמשך ה-cooldown (ארוך = אחרי תנועה רגילה, קצר = אחרי קפיצה).
 *
 * jumpDurationMs ו-shortCooldownMs עדיין לא מוגדרים במסמך הכללים -
 * הם קשורים למנגנון ה-airborne/jump שנשאר בינתיים כמו שהוא (לא נוגעים
 * בו בשלב הזה), ולכן שני הפריסטים משתמשים כרגע באותם ערכים היסטוריים.
 */
public record SpeedConfig(long cellDurationMs, long jumpDurationMs, long longCooldownMs, long shortCooldownMs) {

    public static final SpeedConfig STANDARD =
            new SpeedConfig(1000, 1000, 10_000, 400);

    public static final SpeedConfig LIGHTNING =
            new SpeedConfig(200, 1000, 2_000, 400);
}
