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
        if (newMotion.piece().kind() == Piece.Kind.KNIGHT) return;

        for (Motion existing : currentlyActive) {
            if (existing.piece().kind() == Piece.Kind.KNIGHT) continue;

            if (newMotion.piece().isSameColorAs(existing.piece())) {
                registerFriendlyIfColliding(newMotion, existing, gameClock);
            } else {
                registerEnemyIfColliding(newMotion, existing, gameClock);
            }
        }

        registerStationaryBlockers(newMotion);
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
