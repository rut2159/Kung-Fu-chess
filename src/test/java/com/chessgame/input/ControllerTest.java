package com.chessgame.input;

import com.chessgame.engine.GameEngine;
import com.chessgame.engine.MoveResult;
import com.chessgame.model.Board;
import com.chessgame.model.GameState;
import com.chessgame.io.BoardParser;
import com.chessgame.realtime.RealTimeArbiter;
import com.chessgame.rules.PieceRules;
import com.chessgame.rules.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    private Board board;
    private Controller controller;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wK bR .\n. . .\n. . .");
        GameState gameState = new GameState();
        RuleEngine ruleEngine = new RuleEngine(board, new PieceRules());
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, gameState, ruleEngine, arbiter);
        BoardMapper mapper = new BoardMapper(board);
        controller = new Controller(mapper, engine);
    }

    @Test
    void firstClickOnEmptyCell_isIgnored() {
        ControllerResult result = controller.click(150, 50);

        assertFalse(result.requestedMove());
    }

    @Test
    void firstClickOnAPiece_selectsItWithoutRequestingAMove() {
        ControllerResult result = controller.click(50, 50);

        assertFalse(result.requestedMove());
    }

    @Test
    void secondClickOnEmptyCell_requestsAMove() {
        controller.click(50, 50);
        ControllerResult result = controller.click(50, 150);

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }

    @Test
    void secondClickOnFriendlyPiece_sendsRequestMove_notReselect() {
        board = new BoardParser().parse("wK wR .\n. . .\n. . .");
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
        controller = new Controller(new BoardMapper(board), engine);

        controller.click(50, 50);
        ControllerResult result = controller.click(150, 50);

        assertTrue(result.requestedMove());
        assertFalse(result.moveResult().isAccepted());
        assertEquals(com.chessgame.rules.MoveReason.FRIENDLY_DESTINATION, result.moveResult().reason());
    }

    @Test
    void knightCanCaptureFriendlyPieceThroughController() {
        board = new BoardParser().parse(". . .\n. . wP\n. . .");
        board.addPiece(new com.chessgame.model.Piece("n", com.chessgame.model.Piece.Color.WHITE, com.chessgame.model.Piece.Kind.KNIGHT, new com.chessgame.model.Position(0, 0)));
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(board, new PieceRules()), new RealTimeArbiter(board));
        controller = new Controller(new BoardMapper(board), engine);

        controller.click(50, 50);
        ControllerResult result = controller.click(250, 150);

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }

    @Test
    void clickOutsideBoard_withNoSelection_isIgnored() {
        ControllerResult result = controller.click(-10, -10);

        assertFalse(result.requestedMove());
    }

    @Test
    void clickOutsideBoard_withActiveSelection_cancelsSelectionWithoutSendingACommand() {
        controller.click(50, 50);

        ControllerResult result = controller.click(-10, -10);

        assertFalse(result.requestedMove());

        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove());
    }

    @Test
    void secondClickAlwaysClearsSelection_evenWhenMoveIsRejected() {
        controller.click(50, 50);
        controller.click(250, 250);

        ControllerResult next = controller.click(50, 150);
        assertFalse(next.requestedMove());
    }

    @Test
    void jump_requestsAJumpDirectly() {
        ControllerResult result = controller.jump(150, 50);

        assertTrue(result.requestedMove());
        assertTrue(result.moveResult().isAccepted());
    }
}
