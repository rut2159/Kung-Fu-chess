package com.chessgame.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WelcomeScreenTest {

    @Test
    void constructor_doesNotThrow_onAMachineWithARealDisplay() {
        WelcomeScreen screen = new WelcomeScreen((white, black) -> {
        });

        assertNotNull(screen);
        assertEquals("Kong Fu Chess", screen.getTitle());
    }
}
