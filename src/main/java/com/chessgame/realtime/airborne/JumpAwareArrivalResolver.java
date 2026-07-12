package com.chessgame.realtime.airborne;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.ArrivalResolver;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JumpAwareArrivalResolver {
    private final Board board;
    private final ArrivalResolver commonRouteResolver;
    private final AirborneManager airborneManager;

    public JumpAwareArrivalResolver(Board board, ArrivalResolver commonRouteResolver, AirborneManager airborneManager) {
        this.board = board;
        this.commonRouteResolver = commonRouteResolver;
        this.airborneManager = airborneManager;
    }

    public boolean resolveArrivals(Iterable<Motion> arrivedMotions) {
        List<Motion> remaining = new ArrayList<>();

        for (Motion motion : arrivedMotions) {
            Optional<AirborneMotion> defender = airborneManager.findCapturingJump(motion);
            if (defender.isPresent()) {
                resolveAirborneCapture(motion, defender.get());
            } else {
                remaining.add(motion);
            }
        }

        return commonRouteResolver.resolveArrivals(remaining);
    }

    private void resolveAirborneCapture(Motion motion, AirborneMotion defender) {
        board.removePiece(motion.source());
        motion.piece().setState(Piece.State.CAPTURED);
        airborneManager.consumeJump(defender);
    }
}
