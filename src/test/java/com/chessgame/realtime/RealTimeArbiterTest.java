package com.chessgame.realtime;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RealTimeArbiterTest {

    private Board board;
    private RealTimeArbiter arbiter;

    @BeforeEach
    void setUp() {
        board = new BoardParser().parse("wR . .\n. . .\n. . .");
        arbiter = new RealTimeArbiter(board);
    }

    @Test
    void movingOneSquare_takes1000ms() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        boolean kingCaptured = arbiter.advanceTime(999);
        assertNull(board.pieceAt(new Position(0, 1)));

        kingCaptured |= arbiter.advanceTime(1);
        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertFalse(kingCaptured);
    }

    @Test
    void movingTwoSquares_takes2000ms() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 2));

        arbiter.advanceTime(1000);
        assertNull(board.pieceAt(new Position(0, 2)));

        arbiter.advanceTime(1000);
        assertNotNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void capturingAnEnemyPiece_reportsKingCaptureWhenTargetIsKing() {
        board = new BoardParser().parse("wR bK");
        arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        boolean kingCaptured = arbiter.advanceTime(1000);

        assertTrue(kingCaptured);
    }

    @Test
    void twoSimultaneousMotions_bothArriveIndependently() {
        board = new BoardParser().parse("wR . . bR");
        arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(new Position(0, 0), new Position(0, 1));
        arbiter.startMotion(new Position(0, 3), new Position(0, 2));
        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 1)));
        assertNotNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void canStartMotion_isFalseWhenSourcePieceIsAlreadyMoving() {
        arbiter.startMotion(new Position(0, 0), new Position(0, 1));

        assertFalse(arbiter.canStartMotion(new Position(0, 0), new Position(0, 2)));
    }

    @Test
    void jumpingPiece_capturesAnEnemyThatArrivesWhileAirborne() {
        board = new BoardParser().parse("wK bR .");
        arbiter = new RealTimeArbiter(board);

        arbiter.startJump(new Position(0, 0));
        arbiter.startMotion(new Position(0, 1), new Position(0, 0));

        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 0)));
        assertEquals(Piece.Kind.KING, board.pieceAt(new Position(0, 0)).kind());
        assertNull(board.pieceAt(new Position(0, 1)));
    }

    @Test
    void jumpingPiece_landsNormallyIfNoEnemyArrives() {
        arbiter.startJump(new Position(0, 0));

        arbiter.advanceTime(1000);

        assertNotNull(board.pieceAt(new Position(0, 0)));
        assertFalse(arbiter.canStartMotion(new Position(0, 0), new Position(0, 1)));

        arbiter.advanceTime(400);
        assertTrue(arbiter.canStartMotion(new Position(0, 0), new Position(0, 1)));
    }
}
