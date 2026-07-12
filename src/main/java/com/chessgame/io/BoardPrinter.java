package com.chessgame.io;

import com.chessgame.model.Board;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public final class BoardPrinter {

    public String print(Board board) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Piece piece = board.pieceAt(new Position(row, col));
                sb.append(piece == null ? "." : symbol(piece));
                if (col < board.width() - 1) sb.append(" ");
            }
            if (row < board.height() - 1) sb.append("\n");
        }

        return sb.toString();
    }

    private String symbol(Piece piece) {
        char color = piece.color() == Piece.Color.WHITE ? 'w' : 'b';
        char kind;
        switch (piece.kind()) {
            case KING:   kind = 'K'; break;
            case QUEEN:  kind = 'Q'; break;
            case ROOK:   kind = 'R'; break;
            case BISHOP: kind = 'B'; break;
            case KNIGHT: kind = 'N'; break;
            default:     kind = 'P';
        }
        return "" + color + kind;
    }
}
