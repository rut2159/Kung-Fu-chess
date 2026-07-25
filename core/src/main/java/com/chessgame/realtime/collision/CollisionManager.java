package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CollisionManager {
    private final Board board;
    private final MotionManager motionManager;
    private final long cellDurationMs;
    private final List<CollisionCandidate> pending = new ArrayList<>();

    public CollisionManager(Board board, MotionManager motionManager, long cellDurationMs) {
        this.board = board;
        this.motionManager = motionManager;
        this.cellDurationMs = cellDurationMs;
    }

    public void registerIfColliding(Motion newMotion, List<Motion> currentlyActive, long gameClock) {
        boolean newIsKnight = newMotion.piece().kind() == Piece.Kind.KNIGHT;

        for (Motion existing : currentlyActive) {
            boolean existingIsKnight = existing.piece().kind() == Piece.Kind.KNIGHT;

            if (newIsKnight || existingIsKnight) {
                // הפרש עצמו אף פעם לא נעצר ולא מוגבל - אבל זה לא אמור לבטל
                // את ההגנה של הכלי הידידותי *השני* (הלא-פרש) מפני נחיתה על
                // תא שהפרש כבר תפס. נבדק משני הכיוונים האפשריים: פעם שהפרש
                // הוא זה שכבר פעיל (existing) ופעם שהפרש הוא זה שרק עכשיו
                // התחיל לזוז (newMotion). ראו registerKnightFriendlyIfColliding
                // לפירוט המלא ולמה זה חד-כיווני ולמה אי-אפשר להשתמש כאן
                // ב-findSharedCell הרגיל.
                if (newMotion.piece().isSameColorAs(existing.piece())) {
                    if (existingIsKnight && !newIsKnight) {
                        registerKnightFriendlyIfColliding(newMotion, existing, gameClock);
                    } else if (newIsKnight && !existingIsKnight) {
                        registerKnightFriendlyIfColliding(existing, newMotion, gameClock);
                    }
                    // אם שניהם פרשים - לא נרשם כלום, "פרשים לא מתנגשים" חל
                    // גם ביניהם לבין עצמם.
                }
                continue;
            }

            if (newMotion.piece().isSameColorAs(existing.piece())) {
                registerFriendlyIfColliding(newMotion, existing, gameClock);
            } else {
                registerEnemyIfColliding(newMotion, existing, gameClock);
            }
        }

        // מסלול ה-L של הפרש לא מתאים לגיאומטריית-הנתיב הישר/אלכסוני
        // שמשמשת את registerStationaryBlockers (ותגרום ללולאה אינסופית) -
        // ובכל מקרה הפרש לא אמור להיעצר ע"י חוסמים סטטיים בדרך.
        if (!newIsKnight) {
            registerStationaryBlockers(newMotion);
        }
    }

    // התנגשות ידידותית - נשארת בדיוק כמו שהייתה: זיהוי-תא-בדיד, "הקרוב ממשיך".
    private void registerFriendlyIfColliding(Motion newMotion, Motion existing, long gameClock) {
        Position sharedCell = CollisionGeometry.findSharedCell(newMotion, existing, cellDurationMs);
        if (sharedCell == null) return;

        long timeNew = CollisionGeometry.arrivalTimeAt(newMotion, sharedCell, cellDurationMs);
        long timeExisting = CollisionGeometry.arrivalTimeAt(existing, sharedCell, cellDurationMs);
        long eventTime = Math.min(timeNew, timeExisting);

        // ראו הערה מקבילה למטה ב-registerEnemyIfColliding.
        if (eventTime < gameClock) return;

        pending.add(new FriendlyMotionCollision(eventTime, newMotion, existing, sharedCell, timeNew, timeExisting, cellDurationMs));
    }

    /**
     * פרש-מול-ידידותי-אחר: חד-כיווני בכוונה. הפרש עצמו לעולם לא נעצר ולעולם
     * לא מוגבל - זו בדיוק ההגדרה של "פרש לא מתנגש". אבל אם כלי ידידותי
     * אחר (לא-פרש) עומד לנחות על אותו תא שאליו הפרש הזה כבר בדרך, והפרש
     * מגיע קודם - הכלי השני חייב להיעצר תא אחד לפני, בדיוק כמו מול כל חוסם
     * ידידותי סטטי אחר. אחרת, ברגע שהפרש נוחת (הופך לעומד במקום), הכלי
     * השני ש"לא ידע" על כך היה מגיע ופשוט תופס אותו (ArrivalResolver לא
     * בודק צבע) - וזה בדיוק האיסור: רק פרש מורשה להרוג כלי ידידותי-משלו
     * ע"י נחיתה על תא תפוס, אף כלי אחר לא.
     *
     * אם הסדר הפוך (הכלי השני מגיע ליעד המשותף לפני הפרש) - לא נרשם כאן
     * שום דבר בכוונה: זה בדיוק המקרה שבו הפרש ינחת על תא תפוס ויהרוג את
     * הכלי השני, וזו ההתנהגות הרצויה לפי כללי המשחק, לא תקלה.
     *
     * לא ניתן להשתמש כאן ב-findSharedCell/CollisionGeometry הרגילים (כמו
     * ב-registerFriendlyIfColliding) - הם מניחים מסלול ישר או אלכסוני, ונכנסים
     * ללולאה אינסופית על מסלול ה-L הלא-ישר/לא-אלכסוני של הפרש. הבדיקה כאן
     * משתמשת רק ביעד הסופי של הפרש (לא ב"מסלול" שלו, שאין לו משמעות
     * במונחי המשחק) ובזמני-ההגעה של הכלי השני, שהמסלול שלו-עצמו כן ישר/אלכסוני
     * כרגיל (מובטח כי existing כבר סונן כפרש, ו-newMotion כאן הוא תמיד הלא-פרש).
     */
    private void registerKnightFriendlyIfColliding(Motion otherMotion, Motion knightMotion, long gameClock) {
        Position knightTarget = knightMotion.destination();

        if (!CollisionGeometry.pathPassesThrough(otherMotion, knightTarget)) return;

        long otherArrivalAtTarget = CollisionGeometry.arrivalTimeAt(otherMotion, knightTarget, cellDurationMs);

        // אם הפרש לא מגיע לפני הכלי השני לאותו תא - אין מה לעצור כאן בכלל.
        if (knightMotion.arrivalTime() >= otherArrivalAtTarget) return;
        if (knightMotion.arrivalTime() < gameClock) return;

        pending.add(new KnightFriendlyBlockerCollision(knightMotion.arrivalTime(), otherMotion, knightTarget, cellDurationMs));
    }

    // התנגשות אויב-מול-אויב - זיהוי-רדיוס רציף (0.4 משבצת), "מי-מגיע-אחרון-מנצח".
    private void registerEnemyIfColliding(Motion newMotion, Motion existing, long gameClock) {
        Optional<EnemyProximityDetector.ProximityEvent> proximity = EnemyProximityDetector.findProximityEvent(newMotion, existing);
        if (proximity.isEmpty()) return;

        EnemyProximityDetector.ProximityEvent event = proximity.get();

        // אם רגע-המפגש המחושב כבר עבר ביחס לעכשיו, זו לא התנגשות אמיתית -
        // הכלי הקיים כבר חלף על-פני הנקודה הזו לפני שהתנועה החדשה בכלל
        // התחילה. זו בדיוק הבעיה שראינו בתרחיש המלכה/רץ - בלי הבדיקה הזו,
        // "מפגש" שכבר חלף בזמן עלול "להתפוצץ" מיידית ברגע שכלי חדש נרשם.
        if (event.eventTime() < gameClock) return;

        pending.add(new EnemyMotionCollision(event.eventTime(), event.winner(), event.loser()));
    }

    private void registerStationaryBlockers(Motion newMotion) {
        List<Position> path = CollisionGeometry.pathExcludingDestination(newMotion.source(), newMotion.destination());
        for (Position cell : path) {
            Piece occupant = board.pieceAt(cell);
            if (occupant == null) continue;
            if (motionManager.isPieceMoving(cell)) continue;

            long eventTime = CollisionGeometry.arrivalTimeAt(newMotion, cell, cellDurationMs);

            if (occupant.isEnemyOf(newMotion.piece())) {
                pending.add(new StationaryBlockerCollision(eventTime, newMotion, occupant, cell));
            } else {
                pending.add(new StationaryFriendlyBlockerCollision(eventTime, newMotion, occupant, cell, cellDurationMs));
            }
        }
    }

    public boolean resolveDue(long gameClock) {
        List<CollisionCandidate> due = new ArrayList<>();
        for (CollisionCandidate candidate : pending) {
            if (candidate.eventTime() <= gameClock) due.add(candidate);
        }
        due.sort((a, b) -> Long.compare(a.eventTime(), b.eventTime()));
        pending.removeAll(due);

        boolean kingCaptured = false;
        for (CollisionCandidate candidate : due) {
            if (candidate.isStillRelevant(motionManager, board)) {
                kingCaptured |= candidate.resolve(board, motionManager);
            }
        }
        return kingCaptured;
    }
}
