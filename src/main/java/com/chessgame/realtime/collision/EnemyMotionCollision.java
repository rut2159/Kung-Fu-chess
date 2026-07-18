package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

final class EnemyMotionCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion motionA;
    private final Motion motionB;

    EnemyMotionCollision(long eventTime, Motion motionA, Motion motionB) {
        this.eventTime = eventTime;
        this.motionA = motionA;
        this.motionB = motionB;
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
        boolean aWins = motionA.startTime() < motionB.startTime();
        Motion loser = aWins ? motionB : motionA;

        board.removePiece(loser.source());
        loser.piece().setState(Piece.State.CAPTURED);
        motionManager.remove(loser);

        return loser.piece().kind() == Piece.Kind.KING;
    }
}
