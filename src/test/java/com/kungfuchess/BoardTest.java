package com.kungfuchess;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    @Test
    void getPieceReturnsStaticAndMovingPieces() {
        List<ActiveMove> ongoingMoves = new ArrayList<>();
        ongoingMoves.add(new ActiveMove(0, 0, 1, 0, "bN", 1000));

        Board board = new Board(ongoingMoves);
        board.addRow(new String[] { ".", "." });
        board.addRow(new String[] { ".", "." });

        assertEquals("bN", board.getPiece(0, 0));
        assertEquals(".", board.getPiece(1, 1));
    }

    @Test
    void rowsAndColsCountAreAccurate() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", ".", "." });
        board.addRow(new String[] { "wP", ".", "bK" });

        assertEquals(2, board.getRowsCount());
        assertEquals(3, board.getColsCount());
    }

    @Test
    void setAndClearPieceStaticUpdatesGrid() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", "." });

        board.setPieceStatic(0, 1, "wQ");
        assertEquals("wQ", board.getPiece(0, 1));

        board.clearCellStatic(0, 1);
        assertEquals(".", board.getPiece(0, 1));
    }

    @Test
    void validCellDetectionWorks() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { ".", "." });

        assertTrue(board.isValidCell(0, 0));
        assertFalse(board.isValidCell(2, 0));
        assertFalse(board.isValidCell(0, 2));
    }

    @Test
    void printOutputsBoardState() {
        Board board = new Board(new ArrayList<>());
        board.addRow(new String[] { "wK", "." });
        board.addRow(new String[] { ".", "bP" });

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            board.print();
        } finally {
            System.setOut(originalOut);
        }

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("wK ."));
        assertTrue(printed.contains(". bP"));
    }
}
