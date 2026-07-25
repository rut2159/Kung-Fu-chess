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
import java.util.Optional;

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

        premoveManager.set(source, board.pieceAt(source), destination);
        return MoveResult.premoveQueued();
    }

    private MoveResult executeMove(Position source, Position destination) {
        realTimeArbiter.startMotion(source, destination);
        return MoveResult.accepted();
    }

    /** Called by GameEngine once a motion actually arrives, with the real (not guessed) outcome. */
    public void recordCompletedMove(MoveRecord record) {
        moveHistory.record(record);
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

    /**
     * If a premove is queued for this position, removes it from the queue and
     * returns its destination - the caller (GameEngine) is responsible for
     * actually firing it through requestMove(), so that a premove-triggered
     * move goes through the exact same path (and publishes the exact same
     * events) as any other move.
     */
    public Optional<Position> takeQueuedPremove(Position source) {
        Optional<Position> destination = premoveManager.get(source, board.pieceAt(source));
        destination.ifPresent(ignored -> premoveManager.clear(source));
        return destination;
    }

    public List<MoveRecord> moveHistory() {
        return moveHistory.all();
    }
}
