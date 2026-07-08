package com.kungfuchess;

public class ActiveMove {
    public final int fromRow, fromCol;
    public final int toRow, toCol;
    public final String piece;
    public final long arrivalTime;

    public ActiveMove(int fromRow, int fromCol, int toRow, int toCol, String piece, long arrivalTime) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.arrivalTime = arrivalTime;
    }
}
