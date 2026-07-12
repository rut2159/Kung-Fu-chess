package com.chessgame.engine;

import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Position;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.MoveValidation;
import com.chessgame.rules.RuleEngine;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.model.Piece;

import java.util.ArrayList;
import java.util.List;
public final class GameEngine {
    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter realTimeArbiter;

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter realTimeArbiter) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.realTimeArbiter = realTimeArbiter;
    }

    public com.chessgame.engine.MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return com.chessgame.engine.MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (!realTimeArbiter.canStartMotion(source, destination)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            return MoveResult.rejected(legality.reason());
        }

        realTimeArbiter.startMotion(source, destination);
        return MoveResult.accepted();
    }

    public MoveResult requestJump(Position position) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (!realTimeArbiter.canStartJump(position)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        if (board.pieceAt(position) == null) {
            return MoveResult.rejected(MoveReason.EMPTY_SOURCE);
        }

        realTimeArbiter.startJump(position);
        return MoveResult.accepted();
    }

    public void wait(int milliseconds) {
        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }
    }
    public GameSnapshot snapshot(Position selectedCell) {
        List<GameSnapshot.PieceView> pieces = new ArrayList<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Piece piece = board.pieceAt(new Position(row, col));
                if (piece != null) {
                    pieces.add(new GameSnapshot.PieceView(
                            piece.id(), piece.color(), piece.kind(), piece.cell(), piece.state()));
                }
            }
        }
        return new GameSnapshot(board.width(), board.height(), pieces, selectedCell, gameState.isGameOver());
    }
}
