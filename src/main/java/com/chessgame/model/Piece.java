package com.chessgame.model;

public final class Piece {

    public enum Color {
        WHITE, BLACK
    }

    public enum Kind {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
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