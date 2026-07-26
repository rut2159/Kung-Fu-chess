package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;


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
