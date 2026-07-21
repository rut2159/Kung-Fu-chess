package com.chessgame.realtime.collision;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

/**
 * כלי נע שמגיע לתא שבו יושב כלי ידידותי דומם (לא בתנועה) בנתיב שלו.
 * בניגוד ל-StationaryBlockerCollision (אויב) - כאן אין לכידה: הכלי הנע
 * פשוט נעצר תא אחד לפני החוסם, בדיוק כמו ב-FriendlyMotionCollision.
 * אם החוסם כבר זז משם עד שהגיע הרגע - שום דבר לא קורה (isStillRelevant
 * יחזיר false), והתנועה המקורית פשוט ממשיכה כרגיל.
 */
final class StationaryFriendlyBlockerCollision implements CollisionCandidate {
    private final long eventTime;
    private final Motion movingMotion;
    private final Piece blockerPiece;
    private final Position blockedCell;
    private final long cellDurationMs;

    StationaryFriendlyBlockerCollision(long eventTime, Motion movingMotion, Piece blockerPiece,
                                        Position blockedCell, long cellDurationMs) {
        this.eventTime = eventTime;
        this.movingMotion = movingMotion;
        this.blockerPiece = blockerPiece;
        this.blockedCell = blockedCell;
        this.cellDurationMs = cellDurationMs;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public boolean isStillRelevant(MotionManager motionManager, Board board) {
        return motionManager.isStillActive(movingMotion) && board.pieceAt(blockedCell) == blockerPiece;
    }

    @Override
    public boolean resolve(Board board, MotionManager motionManager) {
        Position restCell = CollisionGeometry.cellBeforeSharedCell(movingMotion, blockedCell);

        if (restCell.equals(movingMotion.source())) {
            motionManager.remove(movingMotion);
            movingMotion.piece().setState(Piece.State.IDLE);
        } else {
            long restArrivalTime = CollisionGeometry.arrivalTimeAt(movingMotion, restCell, cellDurationMs);
            Motion truncated = new Motion(movingMotion.source(), restCell, movingMotion.piece(),
                    movingMotion.startTime(), restArrivalTime);
            motionManager.replace(movingMotion, truncated);
        }

        return false;
    }
}
