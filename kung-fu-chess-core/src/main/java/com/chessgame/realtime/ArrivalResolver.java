package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;

public final class ArrivalResolver {
    private final Board board;

    public ArrivalResolver(Board board) {
        this.board = board;
    }

    public boolean resolveArrivals(Iterable<Motion> arrivedMotions) {
        boolean kingCaptured = false;
        for (Motion motion : arrivedMotions) {
            kingCaptured |= resolveNormalArrival(motion);
        }
        return kingCaptured;
    }

    private boolean resolveNormalArrival(Motion motion) {
        Piece capturedPiece = board.pieceAt(motion.destination());
        board.movePiece(motion.source(), motion.destination());
        motion.piece().setState(Piece.State.IDLE);

        promoteIfEligible(motion);

        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            return capturedPiece.kind() == Piece.Kind.KING;
        }
        return false;
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
