package com.chessgame.model;

import java.util.Optional;

public final class Piece {

    public enum Color {
        WHITE('w'),
        BLACK('b');

        private final char letter;

        Color(char letter) {
            this.letter = letter;
        }

        public char letter() {
            return letter;
        }

        public static Optional<Color> fromLetter(char letter) {
            for (Color color : values()) {
                if (color.letter == letter) {
                    return Optional.of(color);
                }
            }
            return Optional.empty();
        }
    }

    public enum Kind {
        KING('K'),
        QUEEN('Q'),
        ROOK('R'),
        BISHOP('B'),
        KNIGHT('N'),
        PAWN('P');

        private final char letter;

        Kind(char letter) {
            this.letter = letter;
        }

        public char letter() {
            return letter;
        }

        public static Optional<Kind> fromLetter(char letter) {
            for (Kind kind : values()) {
                if (kind.letter == letter) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    public enum State {
        IDLE, MOVING, AIRBORNE, COOLDOWN_LONG, COOLDOWN_SHORT, CAPTURED
    }

    private final String id;
    private final Color color;
    private Kind kind;
    private Position cell;
    private State state;

    public Piece(String id, Color color, Kind kind, Position cell) {
        this.id = id;
        this.color = color;
        this.kind = kind;
        this.cell = cell;
        this.state = State.IDLE;
    }

    public String id() {
        return id;
    }

    public Color color() {
        return color;
    }

    public Kind kind() {
        return kind;
    }

    public Position cell() {
        return cell;
    }

    public State state() {
        return state;
    }

    public void setCell(Position cell) {
        this.cell = cell;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void promoteToQueen() {
        this.kind = Kind.QUEEN;
    }

    public boolean isSameColorAs(Piece other) {
        return this.color == other.color;
    }

    public boolean isEnemyOf(Piece other) {
        return this.color != other.color;
    }

    @Override
    public String toString() {
        return color + " " + kind + " #" + id + " at " + cell + " (" + state + ")";
    }
}
