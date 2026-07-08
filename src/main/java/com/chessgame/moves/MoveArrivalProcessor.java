package com.chessgame.moves;

import com.chessgame.board.Board;
import com.chessgame.pieces.Piece;
import com.chessgame.rules.MoveContext;
import com.chessgame.rules.MoveRuleRegistry;
import com.chessgame.rules.PieceMovementRule;

import java.util.Optional;

/**
 * MoveArrivalProcessor / מעבד הגעות
 *
 * תפקיד: כל הלוגיקה של "מה קורה כשמהלך מגיע ליעד". public כי GameEngine
 * (חבילת השורש) בונה מופע ממנו וקורא ל-processArrivals. משתמש בטיפוסים
 * משלוש חבילות אחרות (board, pieces, rules) - זו בדיוק החבילה ה"מחברת"
 * בין כל השכבות בזמן טיפול בהגעת מהלך.
 */
public final class MoveArrivalProcessor {
    private final Board board;
    private final MoveManager moveManager;
    private final JumpManager jumpManager;
    private final MoveRuleRegistry ruleRegistry;

    public MoveArrivalProcessor(Board board, MoveManager moveManager, JumpManager jumpManager, MoveRuleRegistry ruleRegistry) {
        this.board = board;
        this.moveManager = moveManager;
        this.jumpManager = jumpManager;
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * מעבד את כל המהלכים שהגיעו ליעדם עד לזמן הנתון.
     * מחזיר true אם אחת ההגעות גרמה לסיום המשחק (למשל לכידת מלך).
     */
    public boolean processArrivals(long gameClock) {
        boolean gameEnded = false;
        for (ActiveMove move : moveManager.collectArrived(gameClock)) {
            if (resolveAirborneCapture(move)) {
                continue;
            }
            if (resolveNormalArrival(move)) {
                gameEnded = true;
            }
        }
        return gameEnded;
    }

    /**
     * מטפלת במקרה שבו כלי מרחף עוין נמצא במשבצת היעד - הוא לוכד את
     * הכלי המגיע במקום לתת לו לנחות.
     */
    private boolean resolveAirborneCapture(ActiveMove move) {
        Optional<AirborneMove> defender = jumpManager.findCapturingJump(move);
        if (defender.isEmpty()) return false;

        board.clearCell(move.fromRow, move.fromCol);
        jumpManager.consumeJump(defender.get());
        return true;
    }

    /**
     * מטפלת בהגעה רגילה: מבררת מה יש ביעד, שואלת את חוק התנועה של
     * הכלי מה קורה לו כשהוא מגיע (resolveArrival), ומעדכנת את הלוח.
     */
    private boolean resolveNormalArrival(ActiveMove move) {
        Piece capturedPiece = board.getPiece(move.toRow, move.toCol);

        MoveContext ctx = new MoveContext(board, move.piece, move.fromRow, move.fromCol, move.toRow, move.toCol);
        PieceMovementRule rule = ruleRegistry.ruleFor(move.piece.getType());
        Piece pieceToPlace = rule.resolveArrival(ctx);

        board.setPiece(move.toRow, move.toCol, pieceToPlace);
        board.clearCell(move.fromRow, move.fromCol);

        return !capturedPiece.isEmpty() && ruleRegistry.endsGameWhenCaptured(capturedPiece.getType());
    }
}
