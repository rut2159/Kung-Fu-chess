package com.chessgame.engine;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public final class MoveRecord {
    private final Piece.Color color;
    private final Piece.Kind kind;
    private final Position source;
    private final Position destination;
    private final boolean capture;
    private final long timestamp;

    public MoveRecord(Piece.Color color, Piece.Kind kind, Position source, Position destination,
                       boolean capture, long timestamp) {
        this.color = color;
        this.kind = kind;
        this.source = source;
        this.destination = destination;
        this.capture = capture;
        this.timestamp = timestamp;
    }

    public Piece.Color color() { return color; }
    public Piece.Kind kind() { return kind; }
    public Position source() { return source; }
    public Position destination() { return destination; }

    public boolean isCapture() { return capture; }

    public long timestamp() { return timestamp; }
}
