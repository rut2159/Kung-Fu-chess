package com.chessgame.realtime.collision;

import com.chessgame.realtime.motion.Motion;

import java.util.Optional;

final class EnemyProximityDetector {
    private EnemyProximityDetector() {
    }

    static final double COLLISION_RADIUS_CELLS = 0.4;

    static final class ProximityEvent {
        private final long eventTime;
        private final Motion winner;
        private final Motion loser;

        private ProximityEvent(long eventTime, Motion winner, Motion loser) {
            this.eventTime = eventTime;
            this.winner = winner;
            this.loser = loser;
        }

        long eventTime() { return eventTime; }
        Motion winner() { return winner; }
        Motion loser() { return loser; }
    }

    static Optional<ProximityEvent> findProximityEvent(Motion a, Motion b) {
        long t0 = Math.max(a.startTime(), b.startTime());
        long t1 = Math.min(a.arrivalTime(), b.arrivalTime());
        if (t0 >= t1) return Optional.empty();

        double durA = a.arrivalTime() - a.startTime();
        double durB = b.arrivalTime() - b.startTime();
        double vArow = (a.destination().row() - a.source().row()) / durA;
        double vAcol = (a.destination().col() - a.source().col()) / durA;
        double vBrow = (b.destination().row() - b.source().row()) / durB;
        double vBcol = (b.destination().col() - b.source().col()) / durB;

        double rowA0 = a.source().row() + vArow * (t0 - a.startTime());
        double colA0 = a.source().col() + vAcol * (t0 - a.startTime());
        double rowB0 = b.source().row() + vBrow * (t0 - b.startTime());
        double colB0 = b.source().col() + vBcol * (t0 - b.startTime());

        double dRow0 = rowA0 - rowB0;
        double dCol0 = colA0 - colB0;
        double dvRow = vArow - vBrow;
        double dvCol = vAcol - vBcol;

        double coeffA = dvRow * dvRow + dvCol * dvCol;
        double coeffB = 2 * (dRow0 * dvRow + dCol0 * dvCol);
        double coeffC = dRow0 * dRow0 + dCol0 * dCol0 - COLLISION_RADIUS_CELLS * COLLISION_RADIUS_CELLS;

        double maxS = t1 - t0;
        double entryS;

        if (coeffA == 0) {
            if (coeffC > 0) return Optional.empty();
            entryS = 0;
        } else {
            double discriminant = coeffB * coeffB - 4 * coeffA * coeffC;
            if (discriminant < 0) return Optional.empty();

            double sqrtDisc = Math.sqrt(discriminant);
            double s1 = (-coeffB - sqrtDisc) / (2 * coeffA);
            double s2 = (-coeffB + sqrtDisc) / (2 * coeffA);

            if (s2 < 0 || s1 > maxS) return Optional.empty();
            entryS = Math.max(0, s1);
        }

        long eventTime = t0 + Math.round(entryS);

        Motion winner = (a.startTime() < b.startTime()) ? a : b;
        Motion loser = (winner == a) ? b : a;

        return Optional.of(new ProximityEvent(eventTime, winner, loser));
    }
}
