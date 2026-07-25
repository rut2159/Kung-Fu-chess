package com.chessgame;

import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.GameStartedEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.engine.GameEngine;
import com.chessgame.logging.GameOverSubscriber;
import com.chessgame.logging.GameStartedSubscriber;
import com.chessgame.logging.MoveLogSubscriber;
import com.chessgame.logging.ScoreChangedSubscriber;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.realtime.SpeedConfig;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;

public final class GameSession {
    public final Board board;
    public final GameEngine gameEngine;

    public GameSession(Board board) {
        this(board, SpeedConfig.STANDARD);
    }

    public GameSession(Board board, SpeedConfig speedConfig) {
        this.board = board;

        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board, speedConfig);

        this.gameEngine = new GameEngine(board, gameState, ruleEngine, arbiter);

        wireEventBusSubscribers();
        gameEngine.eventBus().publish(new GameStartedEvent());
    }

    private void wireEventBusSubscribers() {
        MoveLogSubscriber moveLogSubscriber = new MoveLogSubscriber();
        GameOverSubscriber gameOverSubscriber = new GameOverSubscriber();
        ScoreChangedSubscriber scoreChangedSubscriber = new ScoreChangedSubscriber();
        GameStartedSubscriber gameStartedSubscriber = new GameStartedSubscriber();

        gameEngine.eventBus().subscribe(MoveMadeEvent.class, moveLogSubscriber::onMoveMade);
        gameEngine.eventBus().subscribe(GameOverEvent.class, gameOverSubscriber::onGameOver);
        gameEngine.eventBus().subscribe(ScoreChangedEvent.class, scoreChangedSubscriber::onScoreChanged);
        gameEngine.eventBus().subscribe(GameStartedEvent.class, gameStartedSubscriber::onGameStarted);
    }
}
