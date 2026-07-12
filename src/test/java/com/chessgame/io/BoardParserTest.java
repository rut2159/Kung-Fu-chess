package com.chessgame.io;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoardParserTest / טסטים ל-BoardParser
 *
 * פרסינג מלבני, ולידציית טוקן, ומזהי-כלים דטרמיניסטיים - בדיוק מה
 * שהמסמך מגדיר לשכבת Text I/O.
 */
class BoardParserTest {

    private final BoardParser parser = new BoardParser();

    @Test
    void parsesRectangularBoardOfAnySize() {
        Board board = parser.parse("wK . .\n. . .\n. . .\n. . .");

        assertEquals(3, board.width());
        assertEquals(4, board.height());
    }

    @Test
    void whiteAndBlackPrefixes_areParsedCorrectly() {
        Board board = parser.parse("wR bR");

        assertEquals(Piece.Color.WHITE, board.pieceAt(new Position(0, 0)).color());
        assertEquals(Piece.Color.BLACK, board.pieceAt(new Position(0, 1)).color());
    }

    @Test
    void allSixPieceLettersAreRecognized() {
        Board board = parser.parse("wK wQ wR wB wN wP");

        assertEquals(Piece.Kind.KING, board.pieceAt(new Position(0, 0)).kind());
        assertEquals(Piece.Kind.QUEEN, board.pieceAt(new Position(0, 1)).kind());
        assertEquals(Piece.Kind.ROOK, board.pieceAt(new Position(0, 2)).kind());
        assertEquals(Piece.Kind.BISHOP, board.pieceAt(new Position(0, 3)).kind());
        assertEquals(Piece.Kind.KNIGHT, board.pieceAt(new Position(0, 4)).kind());
        assertEquals(Piece.Kind.PAWN, board.pieceAt(new Position(0, 5)).kind());
    }

    @Test
    void dot_meansAnEmptyCell() {
        Board board = parser.parse(". . .");

        assertNull(board.pieceAt(new Position(0, 0)));
        assertNull(board.pieceAt(new Position(0, 1)));
        assertNull(board.pieceAt(new Position(0, 2)));
    }

    @Test
    void invalidToken_throwsUnknownTokenError() {
        BoardParser.BoardParseException ex = assertThrows(
                BoardParser.BoardParseException.class,
                () -> parser.parse("wK X .")
        );

        assertEquals("UNKNOWN_TOKEN", ex.getMessage());
    }

    @Test
    void mismatchedRowWidths_throwRowWidthMismatchError() {
        BoardParser.BoardParseException ex = assertThrows(
                BoardParser.BoardParseException.class,
                () -> parser.parse("wK . .\n. .")   // שורה שנייה קצרה יותר
        );

        assertEquals("ROW_WIDTH_MISMATCH", ex.getMessage());
    }

    @Test
    void pieceIds_areUniqueAndDeterministic() {
        // אותו קלט, שני parse-ים נפרדים - חייבים לתת בדיוק אותם ID-ים,
        // כדי שטסטים יהיו דטרמיניסטיים (חוזרים על עצמם באופן זהה)
        Board board1 = parser.parse("wK bR");
        Board board2 = new BoardParser().parse("wK bR");

        assertEquals(board1.pieceAt(new Position(0, 0)).id(), board2.pieceAt(new Position(0, 0)).id());
        assertNotEquals(
                board1.pieceAt(new Position(0, 0)).id(),
                board1.pieceAt(new Position(0, 1)).id()
        ); // שני כלים שונים באותו לוח - ID-ים שונים
    }
}
