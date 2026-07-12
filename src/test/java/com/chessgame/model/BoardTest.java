package com.chessgame.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoardTest / טסטים ל-Board
 *
 * 6 הטסטים המדויקים שהמסמך דורש: מימדים נכונים, תא ריק מחזיר null,
 * תא תפוס מחזיר את הכלי הנכון, שני כלים באותה משבצת נכשל, הזזה
 * מעדכנת מקור+יעד, הסרת-כלי מנקה תא.
 */
class BoardTest {

    private Board board;

    @BeforeEach
        // רץ מחדש לפני *כל* טסט בנפרד - כל טסט מקבל Board "נקי" בגודל 3x3,
        // כדי שטסטים לא "ידלפו" מצב אחד לשני
    void setUp() {
        board = new Board(3, 3);
    }

    @Test
    void boardDimensions_areInferredCorrectly() {
        assertEquals(3, board.width());
        assertEquals(3, board.height());
    }

    @Test
    void emptyCell_returnsNull() {
        // "no piece" מיוצג כ-null, לא כאובייקט-ריק מיוחד -
        // כי Board הוא Map<Position,Piece>, ותא ריק פשוט לא קיים במפה
        assertNull(board.pieceAt(new Position(1, 1)));
    }

    @Test
    void occupiedCell_returnsTheCorrectPiece() {
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook);

        assertSame(rook, board.pieceAt(new Position(0, 0)));
        // assertSame (לא assertEquals!) - בודק שזה *אותו אובייקט בדיוק*,
        // לא רק "שווה בערכו" - Board לא אמור ליצור עותקים
    }

    @Test
    void addingTwoPiecesToTheSameCell_fails() {
        board.addPiece(new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 1)));

        // assertThrows: מריץ את הקוד בתוך ה-lambda, ומצפה שהוא *יזרוק*
        // בדיוק את החריגה הזו. אם לא נזרקת חריגה - הטסט נכשל.
        assertThrows(IllegalStateException.class, () ->
                board.addPiece(new Piece("p2", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(1, 1)))
        );
    }

    @Test
    void movingAPiece_updatesSourceAndDestination() {
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook);

        board.movePiece(new Position(0, 0), new Position(0, 1));

        assertNull(board.pieceAt(new Position(0, 0)));       // המקור התרוקן
        assertSame(rook, board.pieceAt(new Position(0, 1)));  // אותו כלי, ביעד
        assertEquals(new Position(0, 1), rook.cell());         // הכלי עצמו "יודע" שהוא זז
    }

    @Test
    void removingACapturedPiece_clearsItsCell() {
        board.addPiece(new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(2, 2)));

        board.removePiece(new Position(2, 2));

        assertNull(board.pieceAt(new Position(2, 2)));
    }

    @Test
    void positionOutsideBounds_isNotInBounds() {
        // בדיקת isInBounds - חשוב שזו אחריות של Board, לא Position
        assertFalse(board.isInBounds(new Position(-1, 0)));
        assertFalse(board.isInBounds(new Position(0, 3))); // הלוח 3x3, עמודות 0-2
        assertTrue(board.isInBounds(new Position(2, 2)));  // הפינה האחרונה החוקית
    }
}
