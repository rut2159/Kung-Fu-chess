package com.chessgame;

import com.chessgame.engine.GameEngine;
import com.chessgame.input.BoardMapper;
import com.chessgame.input.Controller;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;

/**
 * GameSession / מפגש-משחק
 *
 * תפקיד: "שורש-ההרכבה" (composition root) - בהינתן Board מוכן, בונה
 * ומחבר פעם אחת את כל שכבות המשחק (RuleEngine, RealTimeArbiter,
 * GameEngine, Controller). זו האחריות היחידה שלה - "מי בונה מה
 * ומעביר למי" - לא קריאת-קלט, לא פרסינג-פקודות.
 */
final class GameSession {
    final Board board;
    final GameEngine gameEngine;
    final Controller controller;

    GameSession(Board board) {
        this.board = board;

        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        this.gameEngine = new GameEngine(board, gameState, ruleEngine, arbiter);
        this.controller = new Controller(board, new BoardMapper(board), gameEngine);
    }
}
