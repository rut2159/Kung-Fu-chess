package com.chessgame.engine;

import com.chessgame.model.Position;

final class MotionInterpolator {
    private MotionInterpolator() {}

    static double[] displayPosition(Position source, Position destination,
                                     long startTime, long arrivalTime, long gameClock) {
        if (destination == null) {
            return new double[]{source.row(), source.col()};
        }

        long total = arrivalTime - startTime;
        double progress = (total <= 0) ? 1.0 : (double) (gameClock - startTime) / total;
        progress = Math.max(0.0, Math.min(1.0, progress));

        double row = source.row() + (destination.row() - source.row()) * progress;
        double col = source.col() + (destination.col() - source.col()) * progress;
        return new double[]{row, col};
    }
}
