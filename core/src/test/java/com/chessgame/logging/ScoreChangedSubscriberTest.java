package com.chessgame.logging;

import com.chessgame.bus.events.ScoreChangedEvent;
import com.chessgame.model.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreChangedSubscriberTest {

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
    void onScoreChanged_printsColorAndNewScore() {
        ScoreChangedSubscriber subscriber = new ScoreChangedSubscriber();

        subscriber.onScoreChanged(new ScoreChangedEvent(Piece.Color.WHITE, 9));

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("WHITE"));
        assertTrue(output.contains("9"));
    }
}
