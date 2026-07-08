package com.kungfuchess;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {
    @Test
    void clickSelectsAndMovesPiece() throws Exception {
        GameEngine engine = new GameEngine();
        Board board = getBoardFromEngine(engine);
        board.addRow(new String[] { "wR", "." });
        board.addRow(new String[] { ".", "." });

        invokeProcessLine(engine, "Commands:");
        invokeProcessLine(engine, "click 0 0");
        invokeProcessLine(engine, "click 0 100");
        invokeProcessLine(engine, "wait 1000");

        assertEquals(".", board.getPiece(0, 0));
        assertEquals("wR", board.getPiece(1, 0));
    }

    @Test
    void printBoardOutputsCurrentState() throws Exception {
        GameEngine engine = new GameEngine();
        Board board = getBoardFromEngine(engine);
        board.addRow(new String[] { "wK", "." });
        board.addRow(new String[] { ".", "bP" });

        invokeProcessLine(engine, "Commands:");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            invokeProcessLine(engine, "print board");
        } finally {
            System.setOut(originalOut);
        }

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("wK ."));
        assertTrue(printed.contains(". bP"));
    }

    @Test
    void waitTriggersArrivalAndPromotion() throws Exception {
        GameEngine engine = new GameEngine();
        Board board = getBoardFromEngine(engine);
        board.addRow(new String[] { ".", "." });
        board.addRow(new String[] { "wP", "." });

        invokeProcessLine(engine, "Commands:");
        invokeProcessLine(engine, "click 0 100");
        invokeProcessLine(engine, "click 0 0");
        invokeProcessLine(engine, "wait 1000");

        assertEquals(".", board.getPiece(1, 0));
        assertEquals("wQ", board.getPiece(0, 0));
    }

    private Board getBoardFromEngine(GameEngine engine) {
        try {
            var field = GameEngine.class.getDeclaredField("board");
            field.setAccessible(true);
            return (Board) field.get(engine);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeProcessLine(GameEngine engine, String line) {
        try {
            Method method = GameEngine.class.getDeclaredMethod("processLine", String.class);
            method.setAccessible(true);
            method.invoke(engine, line);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
