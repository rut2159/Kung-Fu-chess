package com.chessgame.engine;

import com.chessgame.engine.listeners.GameListener;
import com.chessgame.engine.listeners.GameListenerRegistry;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.engine.moves.MoveRequestHandler;
import com.chessgame.engine.moves.MoveResult;
import com.chessgame.engine.premove.PremoveManager;
import com.chessgame.engine.scoring.ScoreCalculator;
import com.chessgame.engine.snapshot.GameSnapshot;
import com.chessgame.engine.snapshot.GameSnapshotFactory;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.RuleEngine;

import java.util.List;

public final class GameEngine {
    private final GameState gameState;
    private final RealTimeArbiter realTimeArbiter;
    private final MoveRequestHandler moveRequestHandler;
    private final GameSnapshotFactory snapshotFactory;
    private final GameListenerRegistry listeners = new GameListenerRegistry();
    private final List<Piece> roster;

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter realTimeArbiter) {
        this.gameState = gameState;
        this.realTimeArbiter = realTimeArbiter;
        this.roster = board.allPieces();

        PremoveManager premoveManager = new PremoveManager();
        this.moveRequestHandler = new MoveRequestHandler(board, gameState, ruleEngine, realTimeArbiter, premoveManager);
        this.snapshotFactory = new GameSnapshotFactory(board, realTimeArbiter, premoveManager, roster);
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    public int score(Piece.Color color) {
        return ScoreCalculator.score(roster, color);
    }

    public MoveResult requestMove(Position source, Position destination) {
        MoveResult result = moveRequestHandler.requestMove(source, destination);
        if (result.isAccepted()) {
            listeners.notifyListeners(this);
        }
        return result;
    }

    public MoveResult requestJump(Position position) {
        MoveResult result = moveRequestHandler.requestJump(position);
        if (result.isAccepted()) {
            listeners.notifyListeners(this);
        }
        return result;
    }
    public List<MoveRecord> moveHistory() {
        return moveRequestHandler.moveHistory();
    }

    public void wait(int milliseconds) {
        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }

        for (Position position : realTimeArbiter.justExpiredCooldownPositions()) {
            moveRequestHandler.firePremoveIfAny(position);
        }

        listeners.notifyListeners(this);
    }

    public GameSnapshot snapshot(Position selectedCell) {
        return snapshotFactory.build(selectedCell, gameState.isGameOver());
    }
}
