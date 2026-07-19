package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

final class EnemyMotionCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion winner;
    private final Motion loser;

    EnemyMotionCollision(long eventTime, Motion winner, Motion loser) {
        this.eventTime = eventTime;
        this.winner = winner;
        this.loser = loser;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public boolean isStillRelevant(MotionManager motionManager, Board board) {
        return motionManager.isStillActive(winner) && motionManager.isStillActive(loser);
    }

    @Override
    public boolean resolve(Board board, MotionManager motionManager) {
        board.removePiece(loser.source());
        loser.piece().setState(Piece.State.CAPTURED);
        motionManager.remove(loser);
        return loser.piece().kind() == Piece.Kind.KING;
    }
}
