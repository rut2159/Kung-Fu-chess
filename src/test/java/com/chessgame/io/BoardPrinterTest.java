package com.chessgame.io;

import com.chessgame.model.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardPrinterTest {

    private final BoardParser parser = new BoardParser();
    private final BoardPrinter printer = new BoardPrinter();

    @Test
    void printingAnEmptyBoard_producesOnlyDots() {
        Board board = parser.parse(". .\n. .");

        assertEquals(". .\n. .", printer.print(board));
    }

    @Test
    void printingAMixedBoard_matchesExpectedFormat() {
        Board board = parser.parse("wK . bR\n. wP .");

        assertEquals("wK . bR\n. wP .", printer.print(board));
    }

    @Test
    void parseAndThenPrint_isARoundTrip() {
        String original = "wK . bR\n. . .\nwN . bK";

        Board board = parser.parse(original);
        String printed = printer.print(board);

        assertEquals(original, printed);
    }

    @Test
    void printReflectsBoardChangesAfterAMove() {
        Board board = parser.parse("wR . .");
        board.movePiece(new com.chessgame.model.Position(0, 0), new com.chessgame.model.Position(0, 1));

        assertEquals(". wR .", printer.print(board));
    }
}
