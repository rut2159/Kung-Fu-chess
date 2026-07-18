package com.chessgame.engine;

final class CooldownInterpolator {
    private CooldownInterpolator() {}

    static double remainingFraction(long startTime, long endTime, long gameClock) {
        long total = endTime - startTime;
        if (total <= 0) {
            return 0.0;
        }
        double elapsed = gameClock - startTime;
        double remaining = 1.0 - (elapsed / total);
        return Math.max(0.0, Math.min(1.0, remaining));
    }
}
