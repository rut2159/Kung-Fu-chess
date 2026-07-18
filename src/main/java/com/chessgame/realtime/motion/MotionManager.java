package com.chessgame.realtime.motion;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class MotionManager {
    private final List<Motion> activeMotions = new ArrayList<>();

    public boolean isPieceMoving(Position position) {
        for (Motion m : activeMotions) {
            if (m.source().equals(position)) return true;
        }
        return false;
    }

    public Motion startMove(Position source, Position destination, Piece piece, long startTime, long arrivalTime) {
        piece.setState(Piece.State.MOVING);
        Motion motion = new Motion(source, destination, piece, startTime, arrivalTime);
        activeMotions.add(motion);
        return motion;
    }

    public void replace(Motion oldMotion, Motion newMotion) {
        if (activeMotions.remove(oldMotion)) {
            activeMotions.add(newMotion);
        }
    }

    public void remove(Motion motion) {
        activeMotions.remove(motion);
    }

    public boolean isStillActive(Motion motion) {
        return activeMotions.contains(motion);
    }

    public List<Motion> activeMotionsSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(activeMotions));
    }

    public List<Motion> collectArrived(long gameClock) {
        List<Motion> arrived = new ArrayList<>();
        Iterator<Motion> it = activeMotions.iterator();
        while (it.hasNext()) {
            Motion m = it.next();
            if (m.hasArrived(gameClock)) {
                arrived.add(m);
                it.remove();
            }
        }
        return arrived;
    }

    public Motion motionOf(Position position) {
        for (Motion m : activeMotions) {
            if (m.source().equals(position)) return m;
        }
        return null;
    }
}
