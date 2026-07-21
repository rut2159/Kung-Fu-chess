package com.chessgame.io;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.ArrayList;
import java.util.List;

public final class BoardParser {

    private static final String TOKEN_PATTERN = "^(\\.|[wb][KQRBNP])$";

    public static final class BoardParseException extends RuntimeException {
        public BoardParseException(String errorCode) {
            super(errorCode);
        }
    }

    public Board parse(String text) {
        List<String[]> rows = splitIntoValidatedRows(text);

        int height = rows.size();
        int width = rows.isEmpty() ? 0 : rows.get(0).length;
        Board board = new Board(width, height);

        for (int row = 0; row < rows.size(); row++) {
            String[] tokens = rows.get(row);
            for (int col = 0; col < tokens.length; col++) {
                addPieceIfPresent(board, tokens[col], row, col);
            }
        }

        return board;
    }

    private List<String[]> splitIntoValidatedRows(String text) {
        List<String[]> rows = new ArrayList<>();
        int expectedCols = -1;

        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String[] tokens = trimmed.split("\\s+");
            for (String token : tokens) {
                if (!token.matches(TOKEN_PATTERN)) {
                    throw new BoardParseException("UNKNOWN_TOKEN");
                }
            }

            if (expectedCols == -1) {
                expectedCols = tokens.length;
            } else if (tokens.length != expectedCols) {
                throw new BoardParseException("ROW_WIDTH_MISMATCH");
            }

            rows.add(tokens);
        }

        return rows;
    }

    private void addPieceIfPresent(Board board, String token, int row, int col) {
        if (token.equals(".")) return;

        Piece.Color color = token.charAt(0) == 'w' ? Piece.Color.WHITE : Piece.Color.BLACK;
        Piece.Kind kind = kindFor(token.charAt(1));
        String id = "r" + row + "c" + col;

        board.addPiece(new Piece(id, color, kind, new Position(row, col)));
    }

    private Piece.Kind kindFor(char letter) {
        switch (letter) {
            case 'K': return Piece.Kind.KING;
            case 'Q': return Piece.Kind.QUEEN;
            case 'R': return Piece.Kind.ROOK;
            case 'B': return Piece.Kind.BISHOP;
            case 'N': return Piece.Kind.KNIGHT;
            case 'P': return Piece.Kind.PAWN;
            default:  throw new BoardParseException("UNKNOWN_TOKEN");
        }
    }
}
