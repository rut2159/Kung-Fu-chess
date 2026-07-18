package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

import java.util.ArrayList;
import java.util.List;

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

    public void registerIfColliding(Motion newMotion, List<Motion> currentlyActive) {
        if (newMotion.piece().kind() == Piece.Kind.KNIGHT) return;

        for (Motion existing : currentlyActive) {
            if (existing.piece().kind() == Piece.Kind.KNIGHT) continue;

            Position sharedCell = CollisionGeometry.findSharedCell(newMotion, existing, cellDurationMs);
            if (sharedCell == null) continue;

            long timeNew = CollisionGeometry.arrivalTimeAt(newMotion, sharedCell, cellDurationMs);
            long timeExisting = CollisionGeometry.arrivalTimeAt(existing, sharedCell, cellDurationMs);
            long eventTime = Math.min(timeNew, timeExisting);

            if (newMotion.piece().isSameColorAs(existing.piece())) {
                pending.add(new FriendlyMotionCollision(eventTime, newMotion, existing, sharedCell, timeNew, timeExisting, cellDurationMs));
            } else {
                pending.add(new EnemyMotionCollision(eventTime, newMotion, existing));
            }
        }

        registerStationaryBlockers(newMotion);
    }

    private void registerStationaryBlockers(Motion newMotion) {
        List<Position> path = CollisionGeometry.pathExcludingDestination(newMotion.source(), newMotion.destination());
        for (Position cell : path) {
            Piece occupant = board.pieceAt(cell);
            if (occupant == null) continue;
            if (motionManager.isPieceMoving(cell)) continue;
            if (!occupant.isEnemyOf(newMotion.piece())) continue;

            long eventTime = CollisionGeometry.arrivalTimeAt(newMotion, cell, cellDurationMs);
            pending.add(new StationaryBlockerCollision(eventTime, newMotion, occupant, cell));
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
