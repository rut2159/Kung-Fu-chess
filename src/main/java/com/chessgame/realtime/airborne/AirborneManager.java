package com.chessgame.realtime.airborne;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AirborneManager {
    private final List<AirborneMotion> activeAirborne = new ArrayList<>();

    public boolean isPieceAirborne(Position position) {
        for (AirborneMotion a : activeAirborne) {
            if (a.cell.equals(position)) return true;
        }
        return false;
    }

    public void startJump(Position position, Piece piece, long landTime) {
        piece.setState(Piece.State.AIRBORNE);
        activeAirborne.add(new AirborneMotion(position, piece, landTime));
    }

    public Optional<AirborneMotion> findCapturingJump(Motion motion) {
        for (AirborneMotion a : activeAirborne) {
            if (a.cell.equals(motion.destination())
                    && a.wasAirborneAt(motion.arrivalTime())
                    && a.piece.isEnemyOf(motion.piece())) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public void consumeJump(AirborneMotion jump) {
        activeAirborne.remove(jump);
        jump.piece.setState(Piece.State.IDLE);
    }

    public List<Piece> landExpiredJumps(long gameClock) {
        List<Piece> landed = new ArrayList<>();
        activeAirborne.removeIf(a -> {
            if (a.hasLanded(gameClock)) {
                a.piece.setState(Piece.State.IDLE);
                landed.add(a.piece);
                return true;
            }
            return false;
        });
        return landed;
    }
}
