package com.chessgame.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
    }

    @Test
    void boardDimensions_areInferredCorrectly() {
        assertEquals(3, board.width());
        assertEquals(3, board.height());
    }

    @Test
    void emptyCell_returnsNull() {
        assertNull(board.pieceAt(new Position(1, 1)));
    }

    @Test
    void occupiedCell_returnsTheCorrectPiece() {
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook);

        assertSame(rook, board.pieceAt(new Position(0, 0)));
    }

    @Test
    void addingTwoPiecesToTheSameCell_fails() {
        board.addPiece(new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 1)));

        assertThrows(IllegalStateException.class, () ->
                board.addPiece(new Piece("p2", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(1, 1)))
        );
    }

    @Test
    void movingAPiece_updatesSourceAndDestination() {
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook);

        board.movePiece(new Position(0, 0), new Position(0, 1));

        assertNull(board.pieceAt(new Position(0, 0)));
        assertSame(rook, board.pieceAt(new Position(0, 1)));
        assertEquals(new Position(0, 1), rook.cell());
    }

    @Test
    void removingACapturedPiece_clearsItsCell() {
        board.addPiece(new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(2, 2)));

        board.removePiece(new Position(2, 2));

        assertNull(board.pieceAt(new Position(2, 2)));
    }

    @Test
    void positionOutsideBounds_isNotInBounds() {
        assertFalse(board.isInBounds(new Position(-1, 0)));
        assertFalse(board.isInBounds(new Position(0, 3)));
        assertTrue(board.isInBounds(new Position(2, 2)));
    }
}
