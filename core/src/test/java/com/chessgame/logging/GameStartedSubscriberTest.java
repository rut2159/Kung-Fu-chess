package com.chessgame.logging;

import com.chessgame.bus.events.GameStartedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStartedSubscriberTest {

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
    void onGameStarted_printsGameStartedMessage() {
        GameStartedSubscriber subscriber = new GameStartedSubscriber();

        subscriber.onGameStarted(new GameStartedEvent());

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("GAME STARTED"));
    }
}
