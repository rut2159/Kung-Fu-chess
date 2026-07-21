package com.chessgame.engine.snapshot;

import com.chessgame.engine.premove.PremoveManager;
import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.realtime.cooldown.CooldownManager;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;

public final class GameSnapshotFactory {
    private final Board board;
    private final RealTimeArbiter realTimeArbiter;
    private final PremoveManager premoveManager;
    private final List<Piece> roster;

    public GameSnapshotFactory(Board board, RealTimeArbiter realTimeArbiter, PremoveManager premoveManager, List<Piece> roster) {
        this.board = board;
        this.realTimeArbiter = realTimeArbiter;
        this.premoveManager = premoveManager;
        this.roster = roster;
    }

    public GameSnapshot build(Position selectedCell, boolean isGameOver) {
        List<GameSnapshot.PieceView> pieces = collectPieceViews();
        Piece.Color winner = isGameOver ? determineWinner() : null;

        return new GameSnapshot(board.width(), board.height(), pieces, selectedCell, isGameOver, winner);
    }

    private List<GameSnapshot.PieceView> collectPieceViews() {
        List<GameSnapshot.PieceView> pieces = new ArrayList<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Piece piece = board.pieceAt(new Position(row, col));
                if (piece != null) {
                    pieces.add(toPieceView(piece));
                }
            }
        }
        return pieces;
    }

    private GameSnapshot.PieceView toPieceView(Piece piece) {
        GameSnapshot.PieceView view;
        if (piece.state() == Piece.State.MOVING) {
            view = movingPieceView(piece);
        } else if (piece.state() == Piece.State.COOLDOWN_LONG || piece.state() == Piece.State.COOLDOWN_SHORT) {
            view = cooldownPieceView(piece);
        } else {
           view = new GameSnapshot.PieceView(piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state());
        }

        return premoveManager.get(piece.cell()).isPresent() ? view.withPremove(true) : view;
    }

    private GameSnapshot.PieceView movingPieceView(Piece piece) {
        Motion motion = realTimeArbiter.motionOf(piece.cell());
        Position destination = (motion != null) ? motion.destination() : null;
        long startTime = (motion != null) ? motion.startTime() : 0;
        long arrivalTime = (motion != null) ? motion.arrivalTime() : 0;

        double[] display = MotionInterpolator.displayPosition(
                piece.cell(), destination, startTime, arrivalTime, realTimeArbiter.gameClock());

        return new GameSnapshot.PieceView(
                piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state(),
                display[0], display[1]);
    }

    private GameSnapshot.PieceView cooldownPieceView(Piece piece) {
        CooldownManager.CooldownWindow window = realTimeArbiter.cooldownOf(piece.cell());
        double remaining = (window != null)
                ? CooldownInterpolator.remainingFraction(window.startTime(), window.endTime(), realTimeArbiter.gameClock())
                : 0.0;

        return new GameSnapshot.PieceView(
                piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state(), remaining);
    }

    private Piece.Color determineWinner() {
        boolean whiteKingAlive = false;
        boolean blackKingAlive = false;

        for (Piece piece : roster) {
            if (piece.kind() != Piece.Kind.KING || piece.state() == Piece.State.CAPTURED) {
                continue;
            }
            if (piece.color() == Piece.Color.WHITE) {
                whiteKingAlive = true;
            } else if (piece.color() == Piece.Color.BLACK) {
                blackKingAlive = true;
            }
        }

        if (whiteKingAlive && !blackKingAlive) return Piece.Color.WHITE;
        if (blackKingAlive && !whiteKingAlive) return Piece.Color.BLACK;
        return null;
    }
}
