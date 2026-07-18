package com.chessgame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void twoPositionsWithSameRowAndCol_areEqual() {
        Position a = new Position(2, 3);
        Position b = new Position(2, 3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void positionsWithDifferentRow_areNotEqual() {
        Position a = new Position(2, 3);
        Position c = new Position(5, 3);

        assertNotEquals(a, c);
    }

    @Test
    void positionsWithDifferentCol_areNotEqual() {
        Position a = new Position(2, 3);
        Position d = new Position(2, 9);

        assertNotEquals(a, d);
    }

    @Test
    void toString_producesReadableRepresentation() {
        Position p = new Position(2, 3);

        assertEquals("(2,3)", p.toString());
    }

    @Test
    void aPositionIsEqualToItself() {
        Position p = new Position(0, 0);

        assertEquals(p, p);
    }
}
