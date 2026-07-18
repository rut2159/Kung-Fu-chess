package com.chessgame.rules;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;
import com.chessgame.io.BoardParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PieceRulesTest {

    private final PieceRules pieceRules = new PieceRules();

    @Test
    void rook_stopsBeforeAFriendlyBlocker() {
        Board board = new BoardParser().parse("wR wP .");
        Piece rook = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, rook);

        assertFalse(destinations.contains(new Position(0, 1)));
        assertFalse(destinations.contains(new Position(0, 2)));
    }

    @Test
    void rook_passesThroughEnemyBlocker_andReachesBeyond() {
        Board board = new BoardParser().parse("wR bP .");
        Piece rook = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, rook);

        assertTrue(destinations.contains(new Position(0, 1)));
        assertTrue(destinations.contains(new Position(0, 2)));
    }

    @Test
    void bishop_movesDiagonallyAndNotStraight() {
        Board board = new BoardParser().parse("wB . .\n. . .\n. . .");
        Piece bishop = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, bishop);

        assertTrue(destinations.contains(new Position(1, 1)));
        assertFalse(destinations.contains(new Position(0, 1)));
        assertFalse(destinations.contains(new Position(1, 0)));
    }

    @Test
    void queen_combinesRookAndBishopMovement() {
        Board board = new BoardParser().parse("wQ . .\n. . .\n. . .");
        Piece queen = board.pieceAt(new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, queen);

        assertTrue(destinations.contains(new Position(0, 2)));
        assertTrue(destinations.contains(new Position(2, 2)));
    }

    @Test
    void knight_jumpsOverBlockers() {
        Board board = new BoardParser().parse("wP wP wP\nwP wN wP\nwP wP wP\n. . .\n. . .");
        Piece knight = board.pieceAt(new Position(1, 1));

        Set<Position> destinations = pieceRules.legalDestinations(board, knight);

        assertTrue(destinations.contains(new Position(3, 0)));
        assertTrue(destinations.contains(new Position(3, 2)));
    }

    @Test
    void knight_canTargetFriendlyOccupiedSquare() {
        Board board = new BoardParser().parse(". . .\n. . wP\n. . .");
        Piece knight = new Piece("n", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(0, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, knight);

        assertTrue(destinations.contains(new Position(1, 2)));
    }

    @Test
    void king_movesOneCellInAnyDirection() {
        Board board = new BoardParser().parse(". . .\n. wK .\n. . .");
        Piece king = board.pieceAt(new Position(1, 1));

        Set<Position> destinations = pieceRules.legalDestinations(board, king);

        assertEquals(8, destinations.size());
    }

    @Test
    void pawn_hasNoTwoStepMoveFromNonStartingRow() {
        Board board = new BoardParser().parse(".\n.\n.\n.\n.\n.");
        board.addPiece(new Piece("p", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 0)));
        Piece pawn = board.pieceAt(new Position(1, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, pawn);

        assertEquals(1, destinations.size());
    }

    @Test
    void pawn_hasDoubleStepFromStartingRow() {
        Board board = new BoardParser().parse(".\n.\nwP\n.");
        Piece pawn = board.pieceAt(new Position(2, 0));

        Set<Position> destinations = pieceRules.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(1, 0)));
        assertTrue(destinations.contains(new Position(0, 0)));
    }
}
