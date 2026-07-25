package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

final class FriendlyMotionCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion motionA;
    private final Motion motionB;
    private final Position sharedCell;
    private final long timeAtCellA;
    private final long timeAtCellB;
    private final long cellDurationMs;

    FriendlyMotionCollision(long eventTime, Motion motionA, Motion motionB, Position sharedCell,
                             long timeAtCellA, long timeAtCellB, long cellDurationMs) {
        this.eventTime = eventTime;
        this.motionA = motionA;
        this.motionB = motionB;
        this.sharedCell = sharedCell;
        this.timeAtCellA = timeAtCellA;
        this.timeAtCellB = timeAtCellB;
        this.cellDurationMs = cellDurationMs;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public boolean isStillRelevant(MotionManager motionManager, Board board) {
        return motionManager.isStillActive(motionA) && motionManager.isStillActive(motionB);
    }

    @Override
    public boolean resolve(Board board, MotionManager motionManager) {
        boolean aArrivesLater = timeAtCellA > timeAtCellB;
        Motion stopping = aArrivesLater ? motionA : motionB;
        Motion continuing = aArrivesLater ? motionB : motionA;

        Position stoppingRestCell = truncate(stopping, sharedCell, motionManager);

        if (CollisionGeometry.pathPassesThrough(continuing, stoppingRestCell)) {
            truncate(continuing, stoppingRestCell, motionManager);
        }

        return false;
    }

    private Position truncate(Motion motion, Position targetCell, MotionManager motionManager) {
        Position restCell = CollisionGeometry.cellBeforeSharedCell(motion, targetCell);

        if (restCell.equals(motion.source())) {
            motionManager.remove(motion);
            motion.piece().setState(Piece.State.IDLE);
            return restCell;
        }

        long restArrivalTime = CollisionGeometry.arrivalTimeAt(motion, restCell, cellDurationMs);
        Motion truncated = new Motion(motion.source(), restCell, motion.piece(), motion.startTime(), restArrivalTime);
        motionManager.replace(motion, truncated);
        return restCell;
    }
}
