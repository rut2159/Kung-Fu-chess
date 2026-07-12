package com.chessgame.rules;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PieceRulesTest / טסטים לחוקי-התנועה
 *
 * לא בודקים כל מחלקת-חוק (RookRule/BishopRule/...) בנפרד - בודקים
 * דרך PieceRules, כי זו נקודת-הכניסה הציבורית האמיתית שכל שאר
 * המערכת (RuleEngine) משתמשת בה. כל טסט בונה לוח קטן וממוקד.
 */
class PieceRulesTest {

    private final PieceRules pieceRules = new PieceRules();

    @Test
    void rook_stopsBeforeAFriendlyBlocker() {
        Board board = new BoardParser().parse("wR wP .");
        Piece rook = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, rook);

        // אין שום דרך להגיע ל-(0,1) [חוסם] או ל-(0,2) [מעבר לחוסם]
        assertFalse(destinations.contains(new Position(0, 1)));
        assertFalse(destinations.contains(new Position(0, 2)));
    }

    @Test
    void rook_capturesAnEnemyBlockerButDoesNotPassIt() {
        Board board = new BoardParser().parse("wR bP .");
        Piece rook = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, rook);

        assertTrue(destinations.contains(new Position(0, 1)));   // אפשר ללכוד
        assertFalse(destinations.contains(new Position(0, 2)));   // אבל לא "מעבר" לכלי שנלכד
    }

    @Test
    void bishop_movesDiagonallyAndNotStraight() {
        Board board = new BoardParser().parse("wB . .\n. . .\n. . .");
        Piece bishop = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, bishop);

        assertTrue(destinations.contains(new Position(1, 1))); // אלכסון - כן
        assertFalse(destinations.contains(new Position(0, 1))); // ישר - לא
        assertFalse(destinations.contains(new Position(1, 0))); // ישר - לא
    }

    @Test
    void queen_combinesRookAndBishopMovement() {
        Board board = new BoardParser().parse("wQ . .\n. . .\n. . .");
        Piece queen = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, queen);

        assertTrue(destinations.contains(new Position(0, 2))); // ישר - כמו רוק
        assertTrue(destinations.contains(new Position(2, 2))); // אלכסון - כמו רץ
    }

    @Test
    void knight_jumpsOverBlockers() {
        // הפרש מוקף לגמרי בכלים ידידותיים - אבל הוא "קופץ" מעליהם,
        // אז זה לא אמור להפריע לו
        Board board = new BoardParser().parse("wP wP wP\nwP wN wP\nwP wP wP\n. . .\n. . .");
        Piece knight = board.pieceAt(new Position(1, 1));

        Set<Position> destinations = pieceRules.legalDestinations(board, knight);

        assertTrue(destinations.contains(new Position(3, 0))); // (1,1)+(2,-1)
        assertTrue(destinations.contains(new Position(3, 2))); // (1,1)+(2,1)
    }

    @Test
    void king_movesOneCellInAnyDirection() {
        Board board = new BoardParser().parse(". . .\n. wK .\n. . .");
        Piece king = board.pieceAt(new Position(1, 1));

        Set<Position> destinations = pieceRules.legalDestinations(board, king);

        assertEquals(8, destinations.size()); // כל 8 המשבצות מסביב, אף אחת רחוקה יותר
    }

    @Test
    void pawn_hasNoTwoStepMoveAndNoPromotion() {
        // בדיוק לפי הדרישה המפורשת: בלי צעד-כפול, בלי קידום (אנחנו
        // עדיין לא ממשנו קידום/צעד-כפול - הטסט הזה מתעד את המצב
        // הנוכחי במפורש)
        Board board = new BoardParser().parse("wP .\n. .\n. .");
        Piece pawn = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, pawn);

        assertEquals(1, destinations.size()); // רק צעד אחד קדימה
        assertTrue(destinations.contains(new Position(1, 0)));
    }
}
