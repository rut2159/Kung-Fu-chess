package com.chessgame.notation;

import com.chessgame.engine.moves.MoveRecord;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public final class MoveNotation {

    private static final char FIRST_FILE = 'a';
    private static final char CAPTURE_MARK = 'x';
    private static final char JUMP_CAPTURE_MARK = '\u2191';
    private static final long MS_PER_MINUTE = 60_000L;
    private static final long MS_PER_SECOND = 1_000L;

    private final int boardRows;

    public MoveNotation(int boardRows) {
        if (boardRows <= 0) {
            throw new IllegalArgumentException("boardRows must be positive, was: " + boardRows);
        }
        this.boardRows = boardRows;
    }

    public static String formatTime(long gameClockMs) {
        long minutes = gameClockMs / MS_PER_MINUTE;
        long seconds = (gameClockMs % MS_PER_MINUTE) / MS_PER_SECOND;
        long millis = gameClockMs % MS_PER_SECOND;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    public String formatMove(MoveRecord move) {
        StringBuilder notation = new StringBuilder();

        if (move.kind() == Piece.Kind.PAWN) {
            if (move.isCapture()) {
                notation.append(fileOf(move.source())).append(CAPTURE_MARK);
            }
        } else {
            notation.append(move.kind().letter());
            if (move.isCapture()) {
                notation.append(CAPTURE_MARK);
            }
        }

        notation.append(algebraic(move.destination()));

        if (move.isJumpCapture()) {
            notation.append(JUMP_CAPTURE_MARK);
        }

        return notation.toString();
    }

    public String algebraic(Position position) {
        return String.valueOf(fileOf(position)) + rankOf(position);
    }

    private static char fileOf(Position position) {
        return (char) (FIRST_FILE + position.col());
    }

    private int rankOf(Position position) {
        return boardRows - position.row();
    }
}
