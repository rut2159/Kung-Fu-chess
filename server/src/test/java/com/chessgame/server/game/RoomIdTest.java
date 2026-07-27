package com.chessgame.server.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomIdTest {

    @Test
    void generatedIdIsSixCharactersLong() {
        assertEquals(6, RoomId.generate().length());
    }

    @Test
    void generatedIdAvoidsCharactersThatAreEasilyMisread() {
        for (int i = 0; i < 500; i++) {
            String id = RoomId.generate();
            for (char c : new char[]{'O', 'I', 'L', '0', '1'}) {
                assertFalse(id.indexOf(c) >= 0, "generated " + id + " containing " + c);
            }
        }
    }

    @Test
    void generatedIdsAreEssentiallyUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(RoomId.generate());
        }
        assertTrue(seen.size() > 1990, "too many collisions: " + seen.size());
    }

    @Test
    void normaliseUppercasesAndTrims() {
        assertEquals("K7F2QX", RoomId.normalise("  k7f2qx "));
    }

    @Test
    void normaliseHandlesNull() {
        assertEquals("", RoomId.normalise(null));
    }

    @Test
    void wellFormedRejectsWrongLengthAndUnknownCharacters() {
        assertTrue(RoomId.isWellFormed(RoomId.generate()));
        assertFalse(RoomId.isWellFormed("ABC"));
        assertFalse(RoomId.isWellFormed("ABCDEFG"));
        assertFalse(RoomId.isWellFormed("ABCDE0"));
        assertFalse(RoomId.isWellFormed(null));
    }
}
