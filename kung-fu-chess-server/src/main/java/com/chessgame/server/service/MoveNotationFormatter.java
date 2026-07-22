package com.chessgame.server.service;

import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

/**
 * Formats a move the same way the desktop client's MoveHistoryFormatter does
 * (algebraic-style notation, mm:ss.mmm timestamps). Duplicated here rather than
 * shared, since that class is desktop-only and server doesn't depend on desktop -
 * it's a small, stable, pure-function helper, so duplication is cheap and keeps
 * the module boundary clean.
 */
final class MoveNotationFormatter {

    private static final int BOARD_ROWS = 8;

    private MoveNotationFormatter() {
    }

    static String formatTime(long gameClockMs) {
        long minutes = gameClockMs / 60000;
        long seconds = (gameClockMs % 60000) / 1000;
        long millis = gameClockMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    static String formatMove(MoveRecord move) {
        String destination = algebraic(move.destination());
        if (move.kind() == Piece.Kind.PAWN) {
            if (move.isCapture()) {
                char sourceFile = (char) ('a' + move.source().col());
                return sourceFile + "x" + destination;
            }
            return destination;
        }

        String pieceLetter = pieceLetter(move.kind());
        String captureMark = move.isCapture() ? "x" : "";
        return pieceLetter + captureMark + destination;
    }

    private static String pieceLetter(Piece.Kind kind) {
        switch (kind) {
            case KING: return "K";
            case QUEEN: return "Q";
            case ROOK: return "R";
            case BISHOP: return "B";
            case KNIGHT: return "N";
            default: return "";
        }
    }

    private static String algebraic(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = BOARD_ROWS - pos.row();
        return "" + file + rank;
    }
}
