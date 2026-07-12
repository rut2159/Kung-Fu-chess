package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.airborne.AirborneManager;
import com.chessgame.realtime.airborne.JumpAwareArrivalResolver;
import com.chessgame.realtime.motion.MotionManager;

public final class RealTimeArbiter {
    private static final int CELL_DURATION_MS = 1000;
    private static final int JUMP_DURATION_MS = 1000;

    private final Board board;
    private final MotionManager motionManager = new MotionManager();
    private final AirborneManager airborneManager = new AirborneManager();
    private final ArrivalResolver commonRouteResolver;
    private final JumpAwareArrivalResolver arrivalResolver;
    private long gameClock = 0;

    public RealTimeArbiter(Board board) {
        this.board = board;
        this.commonRouteResolver = new ArrivalResolver(board);
        this.arrivalResolver = new JumpAwareArrivalResolver(board, commonRouteResolver, airborneManager);
    }

    /**
     * האם מותר להתחיל תנועה ממקור ליעד עכשיו - בודק בבת אחת:
     * הכלי לא כבר בתנועה/מרחף, המסלול לא חוצה תנועה קיימת, היעד
     * לא כבר שמור לתנועה אחרת.
     */
    public boolean canStartMotion(Position source, Position destination) {
        if (motionManager.isPieceMoving(source)) return false;
        if (airborneManager.isPieceAirborne(source)) return false;
        if (motionManager.isPathBlocked(source, destination)) return false;
        if (motionManager.isSquareReserved(destination)) return false;
        return true;
    }

    /** האם מותר להתחיל קפיצה מהתא הנתון עכשיו - הכלי לא כבר בתנועה/מרחף. */
    public boolean canStartJump(Position position) {
        if (motionManager.isPieceMoving(position)) return false;
        if (airborneManager.isPieceAirborne(position)) return false;
        return true;
    }

    public void startMotion(Position source, Position destination) {
        Piece piece = board.pieceAt(source);
        int distance = Math.max(
                Math.abs(destination.row() - source.row()),
                Math.abs(destination.col() - source.col())
        );
        long arrivalTime = gameClock + (long) distance * CELL_DURATION_MS;
        motionManager.startMove(source, destination, piece, arrivalTime);
    }

    public void startJump(Position position) {
        Piece piece = board.pieceAt(position);
        long landTime = gameClock + JUMP_DURATION_MS;
        airborneManager.startJump(position, piece, landTime);
    }

    /** מקדם זמן מדומה. מחזיר true אם הייתה לכידת מלך. */
    public boolean advanceTime(int milliseconds) {
        gameClock += milliseconds;
        boolean kingCaptured = arrivalResolver.resolveArrivals(motionManager.collectArrived(gameClock));
        airborneManager.landExpiredJumps(gameClock);
        return kingCaptured;
    }
}
