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
                if (newMotion.piece().isSameColorAs(existing.piece())) {
                    if (existingIsKnight && !newIsKnight) {
                        registerKnightFriendlyIfColliding(newMotion, existing, gameClock);
                    } else if (newIsKnight && !existingIsKnight) {
                        registerKnightFriendlyIfColliding(existing, newMotion, gameClock);
                    }
                }
                continue;
            }

            if (newMotion.piece().isSameColorAs(existing.piece())) {
                registerFriendlyIfColliding(newMotion, existing, gameClock);
            } else {
                registerEnemyIfColliding(newMotion, existing, gameClock);
            }
        }

        if (!newIsKnight) {
            registerStationaryBlockers(newMotion);
        }
    }

    private void registerFriendlyIfColliding(Motion newMotion, Motion existing, long gameClock) {
        Position sharedCell = CollisionGeometry.findSharedCell(newMotion, existing, cellDurationMs);
        if (sharedCell == null) return;

        long timeNew = CollisionGeometry.arrivalTimeAt(newMotion, sharedCell, cellDurationMs);
        long timeExisting = CollisionGeometry.arrivalTimeAt(existing, sharedCell, cellDurationMs);
        long eventTime = Math.min(timeNew, timeExisting);

        if (eventTime < gameClock) return;

        pending.add(new FriendlyMotionCollision(eventTime, newMotion, existing, sharedCell, timeNew, timeExisting, cellDurationMs));
    }

    private void registerKnightFriendlyIfColliding(Motion otherMotion, Motion knightMotion, long gameClock) {
        Position knightTarget = knightMotion.destination();

        if (!CollisionGeometry.pathPassesThrough(otherMotion, knightTarget)) return;

        long otherArrivalAtTarget = CollisionGeometry.arrivalTimeAt(otherMotion, knightTarget, cellDurationMs);

        if (knightMotion.arrivalTime() >= otherArrivalAtTarget) return;
        if (knightMotion.arrivalTime() < gameClock) return;

        pending.add(new KnightFriendlyBlockerCollision(knightMotion.arrivalTime(), otherMotion, knightTarget, cellDurationMs));
    }

    private void registerEnemyIfColliding(Motion newMotion, Motion existing, long gameClock) {
        Optional<EnemyProximityDetector.ProximityEvent> proximity = EnemyProximityDetector.findProximityEvent(newMotion, existing);
        if (proximity.isEmpty()) return;

        EnemyProximityDetector.ProximityEvent event = proximity.get();


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
