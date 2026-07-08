package com.kungfuchess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {
    @Test
    void gameStateEnumContainsValues() {
        assertEquals(GameState.INIT, GameState.valueOf("INIT"));
        assertEquals(GameState.PARSING_BOARD, GameState.valueOf("PARSING_BOARD"));
        assertEquals(GameState.PARSING_COMMANDS, GameState.valueOf("PARSING_COMMANDS"));
    }
}
