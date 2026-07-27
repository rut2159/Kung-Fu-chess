package com.chessgame.server.game;

import com.chessgame.model.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatsTest {

    @Test
    void firstToArrive_takesWhite() {
        Seats seats = new Seats();
        assertEquals(Seats.Role.WHITE, seats.take("alice"));
    }

    @Test
    void secondToArrive_takesBlack() {
        Seats seats = new Seats();
        seats.take("alice");
        assertEquals(Seats.Role.BLACK, seats.take("bob"));
    }

    @Test
    void everyoneAfterTheSecond_isAViewer() {
        Seats seats = new Seats();
        seats.take("alice");
        seats.take("bob");
        assertEquals(Seats.Role.VIEWER, seats.take("carol"));
        assertEquals(Seats.Role.VIEWER, seats.take("dave"));
    }

    @Test
    void returningPlayer_keepsTheSameSeat() {
        Seats seats = new Seats();
        seats.take("alice");
        seats.take("bob");
        assertEquals(Seats.Role.WHITE, seats.take("alice"));
        assertEquals(Seats.Role.BLACK, seats.take("bob"));
    }

    @Test
    void aViewerReturning_staysAViewer() {
        Seats seats = new Seats();
        seats.take("alice");
        seats.take("bob");
        seats.take("carol");
        assertEquals(Seats.Role.VIEWER, seats.take("carol"));
    }

    @Test
    void roleOf_doesNotHandOutASeat() {
        Seats seats = new Seats();
        assertEquals(Seats.Role.VIEWER, seats.roleOf("alice"));
        assertTrue(seats.whiteUsername().isEmpty());
    }

    @Test
    void whiteOwnsWhitePiecesOnly() {
        assertTrue(Seats.Role.WHITE.owns(Piece.Color.WHITE));
        assertFalse(Seats.Role.WHITE.owns(Piece.Color.BLACK));
    }

    @Test
    void viewerOwnsNothing() {
        assertFalse(Seats.Role.VIEWER.owns(Piece.Color.WHITE));
        assertFalse(Seats.Role.VIEWER.owns(Piece.Color.BLACK));
    }

    @Test
    void bothSeatsFilled_onlyOnceTwoDistinctPlayersArrived() {
        Seats seats = new Seats();
        assertFalse(seats.bothSeatsFilled());
        seats.take("alice");
        assertFalse(seats.bothSeatsFilled());
        seats.take("alice");
        assertFalse(seats.bothSeatsFilled());
        seats.take("bob");
        assertTrue(seats.bothSeatsFilled());
    }
}
