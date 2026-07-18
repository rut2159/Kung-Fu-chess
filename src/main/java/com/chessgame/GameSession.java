package com.chessgame;

import com.chessgame.engine.GameEngine;
import com.chessgame.input.BoardMapper;
import com.chessgame.input.Controller;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.realtime.SpeedConfig;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;

public final class GameSession {
    public final Board board;
    public final GameEngine gameEngine;
    public final Controller controller;

    public GameSession(Board board) {
        this(board, SpeedConfig.STANDARD);
    }

    public GameSession(Board board, SpeedConfig speedConfig) {
        this.board = board;

        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board, speedConfig);

        this.gameEngine = new GameEngine(board, gameState, ruleEngine, arbiter);
        this.controller = new Controller(new BoardMapper(board), gameEngine);
    }
}
