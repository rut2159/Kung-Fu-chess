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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        engine.wait(1000); // MoveMadeEvent now fires at arrival, not at the request itself

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
        engine.wait(1000); // MoveMadeEvent now fires at arrival, not at the request itself

        assertEquals(1, received.size());
        assertFalse(received.get(0).record().isCapture());
    }

    @Test
    void illegalMove_publishesMoveRejectedEvent() {
        setUp("wR bK\n. .");
        List<MoveRejectedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveRejectedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(1, 1));

        assertEquals(1, received.size());
    }

    @Test
    void legalMove_doesNotPublishMoveRejectedEvent() {
        setUp("wR bK\n. .");
        List<MoveRejectedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveRejectedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(1, 0));

        assertTrue(received.isEmpty());
    }

    @Test
    void capturingTheEnemyKing_publishesGameOverEvent() {
        setUp("wR bK\n. .");
        List<GameOverEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(GameOverEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        assertEquals(1, received.size());
    }

    @Test
    void capturingAPiece_publishesScoreChangedEventForTheCapturingColor() {
        setUp("wR bR\n. .");
        List<ScoreChangedEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(ScoreChangedEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);

        assertEquals(1, received.size());
        assertEquals(Piece.Color.WHITE, received.get(0).color());
    }

    /**
     * Regression test: a premove that fires automatically when a cooldown
     * expires used to bypass GameEngine.requestMove() entirely (it called
     * MoveRequestHandler directly), so MoveMadeEvent was never published for
     * it - meaning move history, and any sound tied to that event, silently
     * never fired for premove-triggered moves even though the move itself
     * genuinely happened on the board.
     */
    @Test
    void premoveThatFiresAutomatically_stillPublishesMoveMadeEvent() {
        setUp("wR . . bR");
        List<MoveMadeEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveMadeEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 1));
        engine.wait(1000);
        received.clear();

        engine.requestMove(new Position(0, 1), new Position(0, 3));
        assertTrue(received.isEmpty(), "queueing a premove must not itself publish a move event yet");

        engine.wait(10000);
        engine.wait(2000);

        assertEquals(1, received.size(), "the premove's own execution must publish exactly one MoveMadeEvent");
        assertTrue(received.get(0).record().isCapture(), "it captured the black rook, so isCapture() must be true");
    }

    /**
     * Regression test for a real bug: the capture flag used to be decided at
     * REQUEST time (was the destination occupied when the move was clicked?),
     * not at ARRIVAL time. Since this is real-time chess, the target can
     * flee mid-flight - and when it does, the attacker's move must be
     * reported as a normal (non-capturing) move once it actually lands on
     * the now-empty square, not as a capture.
     */
    @Test
    void targetThatFleesMidFlight_meansTheArrivingMoveIsNotReportedAsACapture() {
        setUp("wR . . bR\n.  . . .");
        List<MoveMadeEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveMadeEvent.class, received::add);

        engine.requestMove(new Position(0, 0), new Position(0, 3));
        engine.wait(1000);

        engine.requestMove(new Position(0, 3), new Position(1, 3));
        engine.wait(3000); // wR finishes its remaining travel and lands on the now-empty square

        MoveMadeEvent whiteRookArrival = received.stream()
                .filter(e -> e.record().color() == Piece.Color.WHITE)
                .findFirst().orElseThrow(() -> new AssertionError("white rook's own arrival was never published"));

        assertFalse(whiteRookArrival.record().isCapture(),
                "the target fled before arrival - this must NOT be recorded as a capture");
        assertNotNull(board.pieceAt(new Position(1, 3)), "the fleeing rook must have survived at its new square");
    }
}
