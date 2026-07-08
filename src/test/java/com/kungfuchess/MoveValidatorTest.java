package com.kungfuchess;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MoveValidatorTest {
    @Test
    void pawnForwardMoveIsValid() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", "." });
        board.addRow(new String[] { ".", "wP" });

        MoveValidator validator = new MoveValidator(board, "wP", 1, 1, 0, 1);
        assertTrue(validator.isValid());
    }

    @Test
    void pawnDoubleStepFromStartIsValid() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", "." });
        board.addRow(new String[] { ".", "wP" });

        MoveValidator validator = new MoveValidator(board, "wP", 1, 1, -1, 1);
        assertFalse(validator.isValid());

        Board board2 = new Board(new ArrayList<>());
        board2.addRow(new String[] { ".", "." });
        board2.addRow(new String[] { ".", "wP" });
        board2.addRow(new String[] { ".", "." });

        MoveValidator validator2 = new MoveValidator(board2, "wP", 2, 1, 0, 1);
        assertTrue(validator2.isValid());
    }

    @Test
    void pawnCaptureIsValid() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", "." });
        board.addRow(new String[] { ".", "wP" });
        board.setPieceStatic(0, 0, "bN");

        MoveValidator validator = new MoveValidator(board, "wP", 1, 1, 0, 0);
        assertTrue(validator.isValid());
    }

    @Test
    void rookMoveRequiresClearPath() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wR", "wP" });
        board.addRow(new String[] { ".", "." });

        MoveValidator validator = new MoveValidator(board, "wR", 0, 0, 1, 0);
        assertTrue(validator.isValid());

        board.setPieceStatic(1, 0, "bP");
        MoveValidator blockedRook = new MoveValidator(board, "wR", 0, 0, 1, 0);
        assertFalse(blockedRook.isValid());
    }

    @Test
    void bishopMoveRequiresDiagonalAndClearPath() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wB", ".", "." });
        board.addRow(new String[] { ".", ".", "." });
        board.addRow(new String[] { ".", ".", "." });

        MoveValidator validator = new MoveValidator(board, "wB", 0, 0, 2, 2);
        assertTrue(validator.isValid());
    }

    @Test
    void queenMoveCanMoveStraightAndDiagonal() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wQ", ".", "." });
        board.addRow(new String[] { ".", ".", "." });
        board.addRow(new String[] { ".", ".", "." });

        MoveValidator straight = new MoveValidator(board, "wQ", 0, 0, 0, 2);
        assertTrue(straight.isValid());

        MoveValidator diagonal = new MoveValidator(board, "wQ", 0, 0, 2, 2);
        assertTrue(diagonal.isValid());
    }

    @Test
    void knightMoveIsValid() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wN", ".", "." });
        board.addRow(new String[] { ".", ".", "." });
        board.addRow(new String[] { ".", ".", "." });

        MoveValidator validator = new MoveValidator(board, "wN", 0, 0, 2, 1);
        assertTrue(validator.isValid());
    }

    @Test
    void invalidSameColorTargetIsRejected() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wR", "wP" });
        board.addRow(new String[] { ".", "." });

        MoveValidator validator = new MoveValidator(board, "wR", 0, 0, 0, 1);
        assertFalse(validator.isValid());
    }
}
