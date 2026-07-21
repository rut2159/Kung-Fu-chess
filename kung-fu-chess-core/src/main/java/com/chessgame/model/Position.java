package com.chessgame.model;

public final class Position {
    private final int row;
    private final int col;
    private static final int HASH_MULTIPLIER = 31;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Position)) return false;
        Position that = (Position) other;
        return this.row == that.row && this.col == that.col;
    }

    @Override
    public int hashCode() {
        return HASH_MULTIPLIER * row + col;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
