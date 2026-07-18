package com.chessgame.engine;

import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.MoveReason;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private Board board;
    private GameState gameState;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wR bK\n. .");
        gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        engine = new GameEngine(board, gameState, ruleEngine, arbiter);
    }

    @Test
    void legalMove_isAccepted() {
        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 0));

        assertTrue(result.isAccepted());
        assertEquals(MoveReason.OK, result.reason());
    }

    @Test
    void illegalMove_isRejectedWithReasonFromRuleEngine() {
        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 1));

        assertFalse(result.isAccepted());
        assertEquals(MoveReason.ILLEGAL_PIECE_MOVE, result.reason());
    }

    @Test
    void whenGameIsAlreadyOver_moveIsRejectedBeforeAnyOtherCheck() {
        gameState.setGameOver(true);

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 0));

        assertFalse(result.isAccepted());
        assertEquals(MoveReason.GAME_OVER, result.reason());
    }

    @Test
    void capturingTheEnemyKing_setsGameOver() {
        engine.requestMove(new Position(0, 0), new Position(0, 1)); // wR -> bK
        assertFalse(gameState.isGameOver());

        engine.wait(1000);

        assertTrue(gameState.isGameOver());
    }

    @Test
    void requestingASecondMoveWhileFirstIsInFlight_isRejected() {
        engine.requestMove(new Position(0, 0), new Position(0, 1));

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(1, 0));

        assertFalse(result.isAccepted());
        assertEquals(MoveReason.MOTION_IN_PROGRESS, result.reason());
    }

    @Test
    void jumpingAnEmptyCell_isRejectedWithEmptySource() {
        MoveResult result = engine.requestJump(new Position(1, 1));

        assertFalse(result.isAccepted());
        assertEquals(MoveReason.EMPTY_SOURCE, result.reason());
    }

    @Test
    void snapshot_reflectsCurrentBoardStateAndSelection() {
        GameSnapshot snapshot = engine.snapshot(new Position(0, 0));

        assertEquals(new Position(0, 0), snapshot.selectedCell());
        assertFalse(snapshot.isGameOver());
        assertEquals(2, snapshot.pieces().size()); // wR + bK
    }
}
