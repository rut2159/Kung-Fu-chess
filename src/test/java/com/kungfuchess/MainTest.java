package com.kungfuchess;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void mainPrintsStartupMessage() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream emptyInput = new ByteArrayInputStream(new byte[0]);

        PrintStream originalOut = System.out;
        var originalIn = System.in;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        System.setIn(emptyInput);
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("--- המשחק פועל בהצלחה! ---"));
    }
}
