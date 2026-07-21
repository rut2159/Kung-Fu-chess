package com.chessgame.model;

import java.util.HashMap;
import java.util.Map;


public final class Board {
    private final int width;
    private final int height;
    private final Map<Position, Piece> occupancy = new HashMap<>();

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean isInBounds(Position position) {
        return position.row() >= 0 && position.row() < height
                && position.col() >= 0 && position.col() < width;
    }

    public void addPiece(Piece piece) {
        Position cell = piece.cell();
        if (occupancy.containsKey(cell)) {
            throw new IllegalStateException("Cell already occupied: " + cell);
        }
        occupancy.put(cell, piece);
    }

    public void removePiece(Position cell) {
        occupancy.remove(cell);
    }

    public Piece pieceAt(Position cell) {
        return occupancy.get(cell);
    }

    public java.util.List<Piece> allPieces() {
        return new java.util.ArrayList<>(occupancy.values());
    }

    public void movePiece(Position from, Position to) {
        Piece piece = occupancy.remove(from);
        if (piece == null) {
            throw new IllegalStateException("No piece at source cell: " + from);
        }
        occupancy.put(to, piece);
        piece.setCell(to);
    }
}
