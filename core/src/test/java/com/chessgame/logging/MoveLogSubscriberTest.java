package com.chessgame.logging;

import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveLogSubscriberTest {

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
    void onMoveMade_printsColorKindSourceAndDestination() {
        MoveLogSubscriber subscriber = new MoveLogSubscriber();
        MoveRecord record = new MoveRecord(
                Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(6, 0), new Position(4, 0),
                false, 0L);

        subscriber.onMoveMade(new MoveMadeEvent(record));

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("WHITE"));
        assertTrue(output.contains("PAWN"));
        assertTrue(output.contains("(6,0)"));
        assertTrue(output.contains("(4,0)"));
    }

    @Test
    void onMoveMade_whenCapture_mentionsCapture() {
        MoveLogSubscriber subscriber = new MoveLogSubscriber();
        MoveRecord record = new MoveRecord(
                Piece.Color.WHITE, Piece.Kind.PAWN,
                new Position(6, 4), new Position(5, 5),
                true, 0L);

        subscriber.onMoveMade(new MoveMadeEvent(record));

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("capture"));
    }
}
