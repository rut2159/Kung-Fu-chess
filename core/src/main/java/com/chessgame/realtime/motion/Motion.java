package com.chessgame.realtime.motion;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public final class Motion {
    private final Position source;
    private final Position destination;
    private final Piece piece;
    private final long startTime;
    private final long arrivalTime;

    public Motion(Position source, Position destination, Piece piece, long startTime, long arrivalTime) {
        this.source = source;
        this.destination = destination;
        this.piece = piece;
        this.startTime = startTime;
        this.arrivalTime = arrivalTime;
    }

    public Position source() { return source; }
    public Position destination() { return destination; }
    public Piece piece() { return piece; }
    public long startTime() { return startTime; }
    public long arrivalTime() { return arrivalTime; }

    boolean hasArrived(long gameClock) { return gameClock >= arrivalTime; }
}
