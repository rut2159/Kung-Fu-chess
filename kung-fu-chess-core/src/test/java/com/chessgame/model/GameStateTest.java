package com.chessgame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void newGameState_isNotGameOver() {
        GameState state = new GameState();

        assertFalse(state.isGameOver());
    }

    @Test
    void setGameOverTrue_marksGameOver() {
        GameState state = new GameState();

        state.setGameOver(true);

        assertTrue(state.isGameOver());
    }

    @Test
    void setGameOver_canBeToggledBackToFalse() {
        GameState state = new GameState();
        state.setGameOver(true);

        state.setGameOver(false);

        assertFalse(state.isGameOver());
    }
}
