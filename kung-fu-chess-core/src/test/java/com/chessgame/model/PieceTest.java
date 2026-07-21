package com.chessgame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PieceTest {

    @Test
    void newPiece_startsIdle() {
        Piece p = new Piece("p1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));

        assertEquals(Piece.State.IDLE, p.state());
    }

    @Test
    void sameColorPieces_areSameColorNotEnemies() {
        Piece a = new Piece("a", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        Piece b = new Piece("b", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 1));

        assertTrue(a.isSameColorAs(b));
        assertFalse(a.isEnemyOf(b));
    }

    @Test
    void differentColorPieces_areEnemiesNotSameColor() {
        Piece a = new Piece("a", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        Piece b = new Piece("b", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(1, 1));

        assertTrue(a.isEnemyOf(b));
        assertFalse(a.isSameColorAs(b));
    }

    @Test
    void setState_updatesState() {
        Piece p = new Piece("p", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(0, 0));

        p.setState(Piece.State.MOVING);

        assertEquals(Piece.State.MOVING, p.state());
    }

    @Test
    void setCell_updatesCell() {
        Piece p = new Piece("p", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(0, 0));

        p.setCell(new Position(2, 3));

        assertEquals(new Position(2, 3), p.cell());
    }

    @Test
    void promoteToQueen_changesKindOnly() {
        Piece p = new Piece("p7", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(3, 3));

        p.promoteToQueen();

        assertEquals(Piece.Kind.QUEEN, p.kind());
        assertEquals(Piece.Color.BLACK, p.color());
        assertEquals("p7", p.id());
        assertEquals(new Position(3, 3), p.cell());
    }
}
