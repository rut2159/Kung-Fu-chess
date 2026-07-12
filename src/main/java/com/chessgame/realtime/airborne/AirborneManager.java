package com.chessgame.realtime.airborne;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AirborneManager {
    private final List<AirborneMotion> activeAirborne = new ArrayList<>();

    /** האם יש כלי מרחף בתא הנתון. */
    public boolean isPieceAirborne(Position position) {
        for (AirborneMotion a : activeAirborne) {
            if (a.cell.equals(position)) return true;
        }
        return false;
    }

    /** מתחיל קפיצה חדשה, ומעדכן את מצב הכלי ל-AIRBORNE. */
    public void startJump(Position position, Piece piece, long landTime) {
        piece.setState(Piece.State.AIRBORNE);
        activeAirborne.add(new AirborneMotion(position, piece, landTime));
    }

    /**
     * מאתר כלי מרחף עוין שנמצא במשבצת היעד של התנועה הנתונה, ושהיה
     * עדיין באוויר ברגע ההגעה - כלומר יש ללכוד את הכלי המגיע.
     */
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

    /** מסמן שקפיצה נתונה "נצרכה" (לכדה כלי מגיע) - הכלי חוזר ל-IDLE. */
    public void consumeJump(AirborneMotion jump) {
        activeAirborne.remove(jump);
        jump.piece.setState(Piece.State.IDLE);
    }

    /** מנחית באופן טבעי כל קפיצה שזמנה פג, בלי שנלכד עליה אף כלי. */
    public void landExpiredJumps(long gameClock) {
        activeAirborne.removeIf(a -> {
            if (a.hasLanded(gameClock)) {
                a.piece.setState(Piece.State.IDLE);
                return true;
            }
            return false;
        });
    }
}
