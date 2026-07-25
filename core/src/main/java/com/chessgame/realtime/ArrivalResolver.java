package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;

public final class ArrivalResolver {
    private final Board board;

    public ArrivalResolver(Board board) {
        this.board = board;
    }

    public List<ArrivalOutcome> resolveArrivals(Iterable<Motion> arrivedMotions) {
        List<ArrivalOutcome> outcomes = new ArrayList<>();
        for (Motion motion : arrivedMotions) {
            outcomes.add(resolveNormalArrival(motion));
        }
        return outcomes;
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
