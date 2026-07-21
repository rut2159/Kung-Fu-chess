package com.chessgame.engine;

import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.io.BoardParser;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests exercise the real GameEngine + EventBus together, to prove what
 * actually gets published for real game situations - not just isolated units.
 */
class GameEngineBusIntegrationTest {

    private Board board;
    private GameState gameState;
    private GameEngine engine;

    private void setUp(String boardText) {
        board = new BoardParser().parse(boardText);
        gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        engine = new GameEngine(board, gameState, ruleEngine, arbiter);
    }

    @Test
    void pawnCapturingDiagonally_publishesMoveMadeEventWithCaptureTrue() {
        setUp(
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  bP .  .\n" +
                ".  .  .  .  wP .  .  .\n" +
                ".  .  .  .  .  .  .  ."
        );
        List<MoveMadeEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveMadeEvent.class, received::add);

        engine.requestMove(new Position(6, 4), new Position(5, 5));

        assertEquals(1, received.size());
        assertTrue(received.get(0).record().isCapture(),
                "A pawn moving diagonally onto an enemy-occupied square must be reported as a capture");
    }

    @Test
    void pawnMovingForwardToEmptyCell_publishesMoveMadeEventWithCaptureFalse() {
        setUp(
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  .  .  .  .\n" +
                ".  .  .  .  wP .  .  .\n" +
                ".  .  .  .  .  .  .  ."
        );
        List<MoveMadeEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveMadeEvent.class, received::add);

        engine.requestMove(new Position(6, 4), new Position(5, 4));

        assertEquals(1, received.size());
        assertFalse(received.get(0).record().isCapture());
    }

    @Test
    void illegalMove_publishesMoveRejectedEvent() {
        setUp("wR bK\n. .");
        List<MoveRejectedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveRejectedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(1, 1)); // illegal rook move

        assertEquals(1, received.size());
    }

    @Test
    void legalMove_doesNotPublishMoveRejectedEvent() {
        setUp("wR bK\n. .");
        List<MoveRejectedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveRejectedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(1, 0)); // legal rook move

        assertTrue(received.isEmpty());
    }

    @Test
    void capturingTheEnemyKing_publishesGameOverEvent() {
        setUp("wR bK\n. .");
        List<GameOverEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(GameOverEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 1)); // wR -> bK
        engine.wait(1000);

        assertEquals(1, received.size());
    }

    @Test
    void capturingAPiece_publishesScoreChangedEventForTheCapturingColor() {
        setUp("wR bR\n. .");
        List<ScoreChangedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(ScoreChangedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 1)); // wR captures bR
        engine.wait(1000);

        assertEquals(1, received.size());
        assertEquals(Piece.Color.WHITE, received.get(0).color());
    }
}
