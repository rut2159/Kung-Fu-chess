package com.chessgame.moves;

import com.chessgame.pieces.Piece;

/**
 * AirborneMove / כלי מרחף
 *
 * תפקיד: מייצג כלי שקפץ ונמצא כרגע באוויר. שימו לב: המחלקה הזו
 * package-private לגמרי (בלי public בכלל!) - אף חבילה אחרת לא יוצרת
 * אותה ישירות; GameEngine קורא ל-jumpManager.startJump(...) וזה
 * JumpManager שיוצר את ה-AirborneMove מבפנים. זו הדגמה נקייה
 * ל-encapsulation: "כלי מרחף" הוא פרט מימוש פנימי לגמרי של חבילת
 * moves, ושום קוד חיצוני לא צריך לדעת שהוא קיים.
 */
final class AirborneMove {
    final int row, col;
    final Piece piece;
    final long landTime;

    AirborneMove(int row, int col, Piece piece, long landTime) {
        this.row = row;
        this.col = col;
        this.piece = piece;
        this.landTime = landTime;
    }

    /** האם הקפיצה עדיין הייתה פעילה (הכלי עדיין באוויר) בזמן הנתון. */
    boolean wasAirborneAt(long time) {
        return landTime >= time;
    }

    /** האם הקפיצה כבר הסתיימה מעצמה, בהינתן זמן המשחק הנוכחי. */
    boolean hasLanded(long gameClock) {
        return gameClock >= landTime;
    }
}
