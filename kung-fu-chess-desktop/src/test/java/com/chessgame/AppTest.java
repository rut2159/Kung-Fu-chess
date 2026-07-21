package com.chessgame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    private InputStream originalIn;
    private PrintStream originalOut;

    @BeforeEach
    void saveOriginalStreams() {
        originalIn = System.in;
        originalOut = System.out;
    }

    @AfterEach
    void restoreOriginalStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void fullPipeline_parsesBoardExecutesCommandsAndPrintsResult() {
        String input = " Board:\nwK . .\n. . .\n. . bK\nCommands:\nclick 50 50\nclick 150 150\nwait 1000\nprint board\n";

        String output = runAppWith(input);

        assertEquals(". . .\n. wK .\n. . bK\n", output);
    }

    @Test
    void collisionAndCooldownWorkThroughTheRealPipeline() {
        String input = " Board:\nwR bP .\nCommands:\nclick 50 50\nclick 250 50\nwait 2000\nprint board\n";

        String output = runAppWith(input);

        assertEquals(". . wR\n", output);
    }

    private String runAppWith(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));

        new App().run();

        System.out.flush();
        String raw = captured.toString(StandardCharsets.UTF_8);
        return raw.replace("\r\n", "\n");
    }
}