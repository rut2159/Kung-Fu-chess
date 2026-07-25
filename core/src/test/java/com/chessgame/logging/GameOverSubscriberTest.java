package com.chessgame.logging;

import com.chessgame.bus.events.GameOverEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameOverSubscriberTest {

    private PrintStream originalErr;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void redirectStdErr() {
        originalErr = System.err;
        captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdErr() {
        System.setErr(originalErr);
    }

    @Test
    void onGameOver_printsGameOverMessage() {
        GameOverSubscriber subscriber = new GameOverSubscriber();

        subscriber.onGameOver(new GameOverEvent());

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("GAME OVER"));
    }
}
