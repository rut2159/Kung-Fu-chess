package com.kungfuchess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveMoveTest {
    @Test
    void activeMoveStoresAllFields() {
        ActiveMove move = new ActiveMove(0, 1, 2, 3, "wQ", 5000L);

        assertEquals(0, move.fromRow);
        assertEquals(1, move.fromCol);
        assertEquals(2, move.toRow);
        assertEquals(3, move.toCol);
        assertEquals("wQ", move.piece);
        assertEquals(5000L, move.arrivalTime);
    }
}
