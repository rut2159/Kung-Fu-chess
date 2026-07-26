package com.chessgame.realtime.airborne;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.ArrivalOutcome;
import com.chessgame.realtime.ArrivalResolver;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JumpAwareArrivalResolver {
    private final Board board;
    private final ArrivalResolver commonRouteResolver;
    private final AirborneManager airborneManager;

    private List<AirborneCapture> justResolvedAirborneCaptures = List.of();

    public JumpAwareArrivalResolver(Board board, ArrivalResolver commonRouteResolver, AirborneManager airborneManager) {
        this.board = board;
        this.commonRouteResolver = commonRouteResolver;
        this.airborneManager = airborneManager;
    }

    public List<AirborneCapture> justResolvedAirborneCaptures() {
        return justResolvedAirborneCaptures;
    }

    public java.util.List<ArrivalOutcome> resolveArrivals(Iterable<Motion> arrivedMotions) {
        List<Motion> remaining = new ArrayList<>();
        List<AirborneCapture> captures = new ArrayList<>();

        for (Motion motion : arrivedMotions) {
            if (board.pieceAt(motion.source()) != motion.piece()
                    || motion.piece().state() == Piece.State.CAPTURED) {
                continue;
            }

            Optional<AirborneMotion> defender = airborneManager.findCapturingJump(motion);
            if (defender.isPresent()) {
                captures.add(resolveAirborneCapture(motion, defender.get()));
            } else {
                remaining.add(motion);
            }
        }

        justResolvedAirborneCaptures = captures;
        return commonRouteResolver.resolveArrivals(remaining);
    }

    private AirborneCapture resolveAirborneCapture(Motion motion, AirborneMotion defender) {
        board.removePiece(motion.source());
        motion.piece().setState(Piece.State.CAPTURED);

        AirborneCapture capture =
                new AirborneCapture(defender.piece, motion.piece(), defender.cell, motion.arrivalTime());

        airborneManager.consumeJump(defender);
        return capture;
    }
}