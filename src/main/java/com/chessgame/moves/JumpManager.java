package com.chessgame.moves;

import com.chessgame.pieces.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JumpManager / מנהל קפיצות
 *
 * תפקיד: "בעלים" בלעדי של רשימת ה-AirborneMove-ים. public כי GameEngine
 * צריך isPieceAirborne/startJump/landExpiredJumps. שימו לב:
 * findCapturingJump ו-consumeJump נשארות package-private בכוונה - הן
 * מחזירות/מקבלות את הטיפוס הפרטי AirborneMove, ורק MoveArrivalProcessor
 * (באותה חבילה) צריך לקרוא להן. GameEngine לא יכול לגעת בהן בכלל -
 * הגבלת הגישה הזו הופכת אפשרית בזכות חלוקה אמיתית לחבילות.
 */
public final class JumpManager {
    private static final long JUMP_DURATION_MS = 1000L;

    private final List<AirborneMove> airborneMoves = new ArrayList<>();

    /** האם יש כלי מרחף בתא הנתון. */
    public boolean isPieceAirborne(int row, int col) {
        for (AirborneMove jump : airborneMoves) {
            if (jump.row == row && jump.col == col) {
                return true;
            }
        }
        return false;
    }

    /** מתחיל קפיצה חדשה עבור הכלי הנתון, למשך JUMP_DURATION_MS. */
    public void startJump(int row, int col, Piece piece, long gameClock) {
        airborneMoves.add(new AirborneMove(row, col, piece, gameClock + JUMP_DURATION_MS));
    }

    /**
     * מאתר כלי מרחף עוין שנמצא במשבצת היעד של המהלך הנתון, ושהיה
     * עדיין באוויר ברגע הגעת המהלך. package-private - קריאה רק
     * מ-MoveArrivalProcessor (אותה חבילה).
     */
    Optional<AirborneMove> findCapturingJump(ActiveMove move) {
        for (AirborneMove jump : airborneMoves) {
            if (jump.row == move.toRow
                    && jump.col == move.toCol
                    && jump.wasAirborneAt(move.arrivalTime)
                    && jump.piece.isEnemyOf(move.piece)) {
                return Optional.of(jump);
            }
        }
        return Optional.empty();
    }

    /** מסמן שקפיצה נתונה "נצרכה". package-private - ר' הסבר ב-findCapturingJump. */
    void consumeJump(AirborneMove jump) {
        airborneMoves.remove(jump);
    }

    /** מנחית באופן טבעי כל קפיצה שזמנה פג, בלי שנלכד עליה אף כלי. */
    public void landExpiredJumps(long gameClock) {
        airborneMoves.removeIf(jump -> jump.hasLanded(gameClock));
    }
}
