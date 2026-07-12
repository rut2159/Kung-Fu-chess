package com.chessgame.realtime.motion;
import com.chessgame.model.Piece;
import com.chessgame.model.Position;

/** Motion / תנועה - דאטה בלבד: מוצא, יעד, כלי, זמן-הגעה. */
public final class Motion {
    final Position source;
    final Position destination;
    final Piece piece;
    final long arrivalTime;

    public Motion(Position source, Position destination, Piece piece, long arrivalTime) {
        this.source = source;
        this.destination = destination;
        this.piece = piece;
        this.arrivalTime = arrivalTime;
    }

    public Position source() { return source; }
    public Position destination() { return destination; }
    public Piece piece() { return piece; }
    public long arrivalTime() { return arrivalTime; }

    boolean hasArrived(long gameClock) { return gameClock >= arrivalTime; }
}
