package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

/**
 * כלי ידידותי (לא-פרש) שהמסלול שלו-עצמו עובר דרך או מסתיים בדיוק בתא
 * שאליו פרש ידידותי נוחת קודם. הכלי הזה חייב להיעצר תא אחד לפני התא הזה -
 * בדיוק כמו מול כל חוסם ידידותי סטטי אחר (ראו StationaryFriendlyBlockerCollision) -
 * כי לפי כללי המשחק רק פרש מורשה "לנחות על תא תפוס ולהרוג את מי שיושב שם";
 * אף כלי אחר לא מורשה לעשות את זה, גם לא בטעות.
 *
 * חד-כיווני בכוונה: הפרש עצמו (blockerKnight) אף פעם לא מוגבל/נעצר ע"י
 * המחלקה הזו - היא משפיעה אך ורק על הכלי הידידותי השני. ראו
 * CollisionManager.registerKnightFriendlyIfColliding להסבר המלא.
 */
final class KnightFriendlyBlockerCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion blockedMotion;
    private final Position knightCell;
    private final long cellDurationMs;

    KnightFriendlyBlockerCollision(long eventTime, Motion blockedMotion, Position knightCell, long cellDurationMs) {
        this.eventTime = eventTime;
        this.blockedMotion = blockedMotion;
        this.knightCell = knightCell;
        this.cellDurationMs = cellDurationMs;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public boolean isStillRelevant(MotionManager motionManager, Board board) {
        // לא בודקים כלום לגבי הפרש עצמו - הוא דטרמיניסטי (אף מנגנון
        // בקוד לא עוצר/מזיז אותו), אז אם הוא נרשם - הוא בהכרח ינחת שם
        // בדיוק ב-eventTime. מספיק לבדוק שהכלי הנחסם עדיין רלוונטי.
        return motionManager.isStillActive(blockedMotion);
    }

    @Override
    public boolean resolve(Board board, MotionManager motionManager) {
        Position restCell = CollisionGeometry.cellBeforeSharedCell(blockedMotion, knightCell);

        if (restCell.equals(blockedMotion.source())) {
            motionManager.remove(blockedMotion);
            blockedMotion.piece().setState(Piece.State.IDLE);
        } else {
            long restArrivalTime = CollisionGeometry.arrivalTimeAt(blockedMotion, restCell, cellDurationMs);
            Motion truncated = new Motion(blockedMotion.source(), restCell, blockedMotion.piece(),
                    blockedMotion.startTime(), restArrivalTime);
            motionManager.replace(blockedMotion, truncated);
        }

        return false;
    }
}
