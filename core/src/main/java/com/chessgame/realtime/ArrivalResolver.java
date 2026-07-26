package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.realtime.motion.MotionManager;

import java.util.ArrayList;
import java.util.List;

public final class ArrivalResolver {
    private final Board board;
    private final MotionManager motionManager;

    public ArrivalResolver(Board board, MotionManager motionManager) {
        this.board = board;
        this.motionManager = motionManager;
    }

    public List<ArrivalOutcome> resolveArrivals(Iterable<Motion> arrivedMotions) {
        List<ArrivalOutcome> outcomes = new ArrayList<>();
        for (Motion motion : arrivedMotions) {
            if (isStale(motion)) continue;
            outcomes.add(resolveNormalArrival(motion));
        }
        return outcomes;
    }

    /**
     * תנועה "מתה": הכלי שלה כבר לא עומד במשבצת המוצא שממנה הוא יצא -
     * כלומר הוא נהרג באמצע הדרך (או שמישהו אחר כבר תפס את המשבצת).
     * בלי הבדיקה הזו, movePiece היה מרים את מי שיושב שם *עכשיו* ומעיף
     * אותו ליעד של הכלי המת, או קורס אם המשבצת ריקה.
     *
     * זו הגנה שנייה: התנועה אמורה כבר להיות מבוטלת ע"י cancelMotionOf
     * ברגע ההריגה. אם בכל זאת הגענו לכאן - עדיף לוותר על התנועה בשקט
     * מאשר להשחית את הלוח.
     */
    private boolean isStale(Motion motion) {
        return board.pieceAt(motion.source()) != motion.piece()
                || motion.piece().state() == Piece.State.CAPTURED;
    }

    private ArrivalOutcome resolveNormalArrival(Motion motion) {
        // Captured BEFORE any mutation below (including promotion, which can
        // change this same piece's kind in place) - this is what a move-log
        // entry should show as "what actually moved", regardless of what it
        // became a moment later.
        Piece.Kind movedKind = motion.piece().kind();

        Piece capturedPiece = board.pieceAt(motion.destination());
        board.movePiece(motion.source(), motion.destination());
        motion.piece().setState(Piece.State.IDLE);

        promoteIfEligible(motion);

        boolean captured = capturedPiece != null;
        boolean kingCaptured = captured && capturedPiece.kind() == Piece.Kind.KING;
        if (captured) {
            capturedPiece.setState(Piece.State.CAPTURED);
            // הקורבן יכול היה להיות באמצע תנועה משלו (נאכל בדיוק על משבצת
            // המוצא שממנה הוא ממריא) - התנועה הזו חייבת למות איתו.
            motionManager.cancelMotionOf(capturedPiece);
        }

        return new ArrivalOutcome(motion, movedKind, captured, kingCaptured);
    }

    private void promoteIfEligible(Motion motion) {
        Piece piece = motion.piece();
        if (piece.kind() != Piece.Kind.PAWN) return;

        boolean singleStep = Math.abs(motion.source().row() - motion.destination().row()) == 1;
        if (!singleStep) return;

        int backRank = (piece.color() == Piece.Color.WHITE) ? 0 : board.height() - 1;
        if (piece.cell().row() == backRank) {
            piece.promoteToQueen();
        }
    }
}
