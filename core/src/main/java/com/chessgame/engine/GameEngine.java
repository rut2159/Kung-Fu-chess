package com.chessgame.engine;

import com.chessgame.bus.EventBus;
import com.chessgame.bus.SimpleEventBus;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
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
import com.chessgame.realtime.ArrivalOutcome;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.realtime.airborne.AirborneCapture;
import com.chessgame.realtime.motion.Motion;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.RuleEngine;

import java.util.List;

public final class GameEngine {
    private final GameState gameState;
    private final RealTimeArbiter realTimeArbiter;
    private final MoveRequestHandler moveRequestHandler;
    private final GameSnapshotFactory snapshotFactory;
    private final GameListenerRegistry listeners = new GameListenerRegistry();
    private final EventBus eventBus = new SimpleEventBus();
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

    public EventBus eventBus() {
        return eventBus;
    }

    public int score(Piece.Color color) {
        return ScoreCalculator.score(roster, color);
    }

    public MoveResult requestMove(Position source, Position destination) {
        MoveResult result = moveRequestHandler.requestMove(source, destination);
        if (result.isAccepted()) {
            // Deliberately does NOT publish MoveMadeEvent here: acceptance only
            // means the motion has started, not that its outcome (in
            // particular, whether it's a capture) is known yet - the target
            // can still escape mid-flight. See wait() for where the real,
            // arrival-time outcome gets published.
            listeners.notifyListeners(this);
        } else if (result.reason() != MoveReason.PREMOVE_QUEUED) {
            eventBus.publish(new MoveRejectedEvent(result.reason()));
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
        int whiteScoreBefore = score(Piece.Color.WHITE);
        int blackScoreBefore = score(Piece.Color.BLACK);

        boolean kingCaptured = realTimeArbiter.advanceTime(milliseconds);
        if (kingCaptured) {
            gameState.setGameOver(true);
        }

        for (ArrivalOutcome outcome : realTimeArbiter.justResolvedArrivals()) {
            Motion motion = outcome.motion();
            MoveRecord record = new MoveRecord(
                    motion.piece().color(), outcome.movedKind(),
                    motion.source(), motion.destination(),
                    outcome.captured(), realTimeArbiter.gameClock());
            moveRequestHandler.recordCompletedMove(record);
            eventBus.publish(new MoveMadeEvent(record));
        }

        for (AirborneCapture capture : realTimeArbiter.justResolvedAirborneCaptures()) {
            MoveRecord record = new MoveRecord(
                    capture.defender().color(), capture.defender().kind(),
                    capture.cell(), capture.cell(),
                    true, true, capture.timestamp());
            moveRequestHandler.recordCompletedMove(record);
            eventBus.publish(new MoveMadeEvent(record));
        }

        for (Position position : realTimeArbiter.justExpiredCooldownPositions()) {
            moveRequestHandler.takeQueuedPremove(position)
                    .ifPresent(destination -> requestMove(position, destination));
        }

        publishScoreChangeIfAny(Piece.Color.WHITE, whiteScoreBefore);
        publishScoreChangeIfAny(Piece.Color.BLACK, blackScoreBefore);
        if (kingCaptured) {
            eventBus.publish(new GameOverEvent());
        }

        listeners.notifyListeners(this);
    }

    private void publishScoreChangeIfAny(Piece.Color color, int scoreBefore) {
        int scoreAfter = score(color);
        if (scoreAfter != scoreBefore) {
            eventBus.publish(new ScoreChangedEvent(color, scoreAfter));
        }
    }

    public GameSnapshot snapshot(Position selectedCell) {
        return snapshotFactory.build(selectedCell, gameState.isGameOver());
    }
}