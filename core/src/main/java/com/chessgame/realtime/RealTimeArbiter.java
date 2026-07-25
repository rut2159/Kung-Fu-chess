package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.airborne.AirborneManager;
import com.chessgame.realtime.airborne.JumpAwareArrivalResolver;
import com.chessgame.realtime.collision.CollisionManager;
import com.chessgame.realtime.cooldown.CooldownManager;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

import java.util.ArrayList;
import java.util.List;

public final class RealTimeArbiter {

    private static final int MAX_STEP_MS = 25;
    private final Board board;
    private final SpeedConfig speedConfig;
    private final MotionManager motionManager = new MotionManager();
    private final AirborneManager airborneManager = new AirborneManager();
    private final CollisionManager collisionManager;
    private final CooldownManager cooldownManager;
    private final ArrivalResolver commonRouteResolver;
    private final JumpAwareArrivalResolver arrivalResolver;
    private long gameClock = 0;
    private List<Position> justExpiredCooldownPositions = List.of();
    private List<ArrivalOutcome> justResolvedArrivals = List.of();

    public RealTimeArbiter(Board board) {
        this(board, SpeedConfig.STANDARD);
    }

    public RealTimeArbiter(Board board, SpeedConfig speedConfig) {
        this.board = board;
        this.speedConfig = speedConfig;
        this.cooldownManager = new CooldownManager(speedConfig);
        this.collisionManager = new CollisionManager(board, motionManager, speedConfig.cellDurationMs());
        this.commonRouteResolver = new ArrivalResolver(board);
        this.arrivalResolver = new JumpAwareArrivalResolver(board, commonRouteResolver, airborneManager);
    }

    public boolean canStartMotion(Position source, Position destination) {
        if (motionManager.isPieceMoving(source)) return false;
        if (airborneManager.isPieceAirborne(source)) return false;
        if (cooldownManager.isPieceCoolingDown(source)) return false;
        return true;
    }

    public boolean isPieceCoolingDown(Position position) {
        return cooldownManager.isPieceCoolingDown(position);
    }

    /** התאים שהקירור שלהם הסתיים בדיוק בקריאה האחרונה ל-advanceTime - לשימוש ע"י premove. */
    public List<Position> justExpiredCooldownPositions() {
        return justExpiredCooldownPositions;
    }

    /** מה שבאמת קרה לכל תזוזה שהגיעה ליעדה בקריאה האחרונה ל-advanceTime - האמת היחידה לגבי capture. */
    public List<ArrivalOutcome> justResolvedArrivals() {
        return justResolvedArrivals;
    }

    public long gameClock() {
        return gameClock;
    }

    public Motion motionOf(Position source) {
        return motionManager.motionOf(source);
    }

    public CooldownManager.CooldownWindow cooldownOf(Position position) {
        return cooldownManager.cooldownOf(position);
    }

    public boolean canStartJump(Position position) {
        if (motionManager.isPieceMoving(position)) return false;
        if (airborneManager.isPieceAirborne(position)) return false;
        if (cooldownManager.isPieceCoolingDown(position)) return false;
        return true;
    }

    public void startMotion(Position source, Position destination) {
        Piece piece = board.pieceAt(source);
        int distance = Math.max(
                Math.abs(destination.row() - source.row()),
                Math.abs(destination.col() - source.col())
        );
        long arrivalTime = gameClock + (long) distance * speedConfig.cellDurationMs();

        List<Motion> othersBefore = motionManager.activeMotionsSnapshot();
        Motion motion = motionManager.startMove(source, destination, piece, gameClock, arrivalTime);
        collisionManager.registerIfColliding(motion, othersBefore, gameClock);
    }

    public void startJump(Position position) {
        Piece piece = board.pieceAt(position);
        long landTime = gameClock + speedConfig.jumpDurationMs();
        airborneManager.startJump(position, piece, landTime);
    }

    public boolean advanceTime(int milliseconds) {
        if (milliseconds <= 0) {
            return advanceTimeStep(milliseconds);
        }

        boolean kingCaptured = false;
        List<Position> accumulatedExpired = new ArrayList<>();
        List<ArrivalOutcome> accumulatedArrivals = new ArrayList<>();
        int remaining = milliseconds;

        while (remaining > 0) {
            int step = Math.min(remaining, MAX_STEP_MS);
            kingCaptured |= advanceTimeStep(step);
            accumulatedExpired.addAll(justExpiredCooldownPositions);
            accumulatedArrivals.addAll(justResolvedArrivals);
            remaining -= step;
        }

        justExpiredCooldownPositions = accumulatedExpired;
        justResolvedArrivals = accumulatedArrivals;
        return kingCaptured;
    }

    private boolean advanceTimeStep(int milliseconds) {
        gameClock += milliseconds;

        boolean kingCapturedByCollision = collisionManager.resolveDue(gameClock);

        List<Motion> arrived = motionManager.collectArrived(gameClock);
        List<ArrivalOutcome> outcomes = arrivalResolver.resolveArrivals(arrived);
        justResolvedArrivals = outcomes;
        boolean kingCapturedByArrival = outcomes.stream().anyMatch(ArrivalOutcome::kingCaptured);

        for (Motion motion : arrived) {
            if (motion.piece().state() == Piece.State.IDLE) {
                cooldownManager.startLongCooldown(motion.piece(), gameClock);
            }
        }

        List<Piece> landedJumps = airborneManager.landExpiredJumps(gameClock);
        for (Piece piece : landedJumps) {
            if (piece.state() == Piece.State.IDLE) {
                cooldownManager.startShortCooldown(piece, gameClock);
            }
        }

        justExpiredCooldownPositions = cooldownManager.clearExpiredCooldowns(gameClock);

        return kingCapturedByCollision || kingCapturedByArrival;
    }
}
