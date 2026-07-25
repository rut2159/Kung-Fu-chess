package com.chessgame.realtime.collision;

import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;

/**
 * עזרי-נתיב בדידים (רשימת-תאים) - משמשים את ההתנגשות הידידותית (שני
 * כלים נעים) ואת חוסמים-דוממים (אויב/ידידותי). לזיהוי-רדיוס-רציף בין
 * שני כלי-אויב ראו EnemyProximityDetector - שאלה גיאומטרית שונה לגמרי.
 */
final class CollisionGeometry {
    private CollisionGeometry() {}

    static List<Position> pathExcludingDestination(Position source, Position destination) {
        List<Position> full = fullPathInclusive(source, destination);
        return full.subList(0, full.size() - 1);
    }

    /**
     * הנתיב של כלי, בלי משבצת-המוצא שלו עצמו. חשוב עבור findSharedCell:
     * כלי ש"עוזב" את המשבצת שלו נחשב, לפי fullPathInclusive, כמי ש"הגיע"
     * אליה מיידית (מרחק 0, ברגע ההתחלה) - עובדה טריוויאלית שגורמת לכל כלי
     * אחר שעובר דרך אותה משבצת (אפילו הרבה אחרי שהראשון כבר עזב אותה
     * בפועל) להיראות כאילו "הגיע מאוחר יותר" ולכן נעצר/מבוטל, גם כשהראשון
     * כבר מזמן לא שם. לכן, משבצת-המוצא לא נחשבת "חוסמת" בכלל.
     */
    private static List<Position> pathExcludingSource(Position source, Position destination) {
        List<Position> full = fullPathInclusive(source, destination);
        return full.subList(1, full.size());
    }

    static Position findSharedCell(Motion a, Motion b, long cellDurationMs) {
        List<Position> pathA = pathExcludingSource(a.source(), a.destination());
        List<Position> pathB = pathExcludingSource(b.source(), b.destination());

        Position bestCell = null;
        long bestTimeDiff = Long.MAX_VALUE;

        for (Position cell : pathA) {
            if (!pathB.contains(cell)) continue;

            long timeA = arrivalTimeAt(a, cell, cellDurationMs);
            long timeB = arrivalTimeAt(b, cell, cellDurationMs);
            long timeDiff = Math.abs(timeA - timeB);

            if (timeDiff < bestTimeDiff) {
                bestTimeDiff = timeDiff;
                bestCell = cell;
            }
        }
        return bestCell;
    }

    static long arrivalTimeAt(Motion motion, Position cell, long cellDurationMs) {
        int distance = chebyshevDistance(motion.source(), cell);
        return motion.startTime() + distance * cellDurationMs;
    }

    /**
     * האם המסלול-העתידי-בפועל של הכלי (לא כולל את משבצת-המוצא הטריוויאלית
     * שלו) עובר דרך התא הזה. באג דומה למה שתיקנו ב-findSharedCell: בלי
     * להוציא את המוצא, כל כלי "עובר" (טריוויאלית) דרך המשבצת שהוא עצמו
     * עומד בה - מה שגרם ב-FriendlyMotionCollision.resolve() לקטוע-מחדש
     * בטעות את הכלי ה"ממשיך" בחזרה למשבצת-המוצא שלו-עצמו, כשה"רחוק"
     * נעצר בדיוק שם.
     */
    static boolean pathPassesThrough(Motion motion, Position cell) {
        return pathExcludingSource(motion.source(), motion.destination()).contains(cell);
    }

    static Position cellBeforeSharedCell(Motion motion, Position sharedCell) {
        int stepRow = Integer.compare(motion.destination().row(), motion.source().row());
        int stepCol = Integer.compare(motion.destination().col(), motion.source().col());
        return new Position(sharedCell.row() - stepRow, sharedCell.col() - stepCol);
    }

    private static List<Position> fullPathInclusive(Position from, Position to) {
        int stepRow = Integer.compare(to.row(), from.row());
        int stepCol = Integer.compare(to.col(), from.col());
        List<Position> path = new ArrayList<>();
        int row = from.row(), col = from.col();
        while (row != to.row() || col != to.col()) {
            path.add(new Position(row, col));
            row += stepRow; col += stepCol;
        }
        path.add(new Position(to.row(), to.col()));
        return path;
    }

    private static int chebyshevDistance(Position a, Position b) {
        return Math.max(Math.abs(a.row() - b.row()), Math.abs(a.col() - b.col()));
    }
}
