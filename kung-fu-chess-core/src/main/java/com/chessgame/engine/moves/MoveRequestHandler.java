package com.chessgame.engine.moves;

import com.chessgame.engine.premove.PremoveManager;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.MoveValidation;
import com.chessgame.rules.RuleEngine;

import java.util.List;

public final class MoveRequestHandler {
    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter realTimeArbiter;
    private final PremoveManager premoveManager;
    private final MoveHistory moveHistory = new MoveHistory();

    public MoveRequestHandler(Board board, GameState gameState, RuleEngine ruleEngine,
                        RealTimeArbiter realTimeArbiter, PremoveManager premoveManager) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.realTimeArbiter = realTimeArbiter;
        this.premoveManager = premoveManager;
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return MoveResult.rejected(MoveReason.GAME_OVER);
        }

        if (realTimeArbiter.isPieceCoolingDown(source)) {
            return handlePremoveRequest(source, destination);
        }

        if (!realTimeArbiter.canStartMotion(source, destination)) {
            return MoveResult.rejected(MoveReason.MOTION_IN_PROGRESS);
        }

        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            return MoveResult.rejected(legality.reason());
        }

        return executeMove(source, destination);
    }

    private MoveResult handlePremoveRequest(Position source, Position destination) {
        MoveValidation legality = ruleEngine.validateMove(source, destination);
        if (!legality.isValid()) {
            premoveManager.clear(source);
            return MoveResult.rejected(legality.reason());
        }

        premoveManager.set(source, destination);
        return MoveResult.premoveQueued();
    }

    private MoveResult executeMove(Position source, Position destination) {
        Piece piece = board.pieceAt(source);
        boolean capture = board.pieceAt(destination) != null;
        long timestamp = realTimeArbiter.gameClock();

        realTimeArbiter.startMotion(source, destination);
        moveHistory.record(new MoveRecord(piece.color(), piece.kind(), source, destination, capture, timestamp));
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

    public void firePremoveIfAny(Position source) {
        premoveManager.get(source).ifPresent(destination -> {
            premoveManager.clear(source);
            requestMove(source, destination);
        });
    }

    public List<MoveRecord> moveHistory() {
        return moveHistory.all();
    }
}
