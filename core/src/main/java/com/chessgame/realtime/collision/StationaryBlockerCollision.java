package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

final class StationaryBlockerCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion movingMotion;
    private final Piece stationaryPiece;
    private final Position cell;

    StationaryBlockerCollision(long eventTime, Motion movingMotion, Piece stationaryPiece, Position cell) {
        this.eventTime = eventTime;
        this.movingMotion = movingMotion;
        this.stationaryPiece = stationaryPiece;
        this.cell = cell;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public boolean isStillRelevant(MotionManager motionManager, Board board) {
        return motionManager.isStillActive(movingMotion) && board.pieceAt(cell) == stationaryPiece;
    }

    @Override
    public boolean resolve(Board board, MotionManager motionManager) {
        board.removePiece(cell);
        stationaryPiece.setState(Piece.State.CAPTURED);
        return stationaryPiece.kind() == Piece.Kind.KING;
    }
}
