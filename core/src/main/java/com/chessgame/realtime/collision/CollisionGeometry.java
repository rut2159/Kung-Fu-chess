package com.chessgame.realtime.collision;

import com.chessgame.model.Position;
import com.chessgame.realtime.motion.Motion;

import java.util.ArrayList;
import java.util.List;

final class CollisionGeometry {
    private static final int MAX_PATH_LENGTH = 1024;

    private CollisionGeometry() {}

    static List<Position> pathExcludingDestination(Position source, Position destination) {
        List<Position> full = fullPathInclusive(source, destination);
        return full.subList(0, full.size() - 1);
    }

    private static List<Position> pathExcludingSource(Position source, Position destination) {
        List<Position> full = fullPathInclusive(source, destination);
        return full.subList(1, full.size());
    }

    static Position findSharedCell(Motion a, Motion b, long cellDurationMs) {
        List<Position> pathA = pathExcludingSource(a.source(), a.destination());
        List<Position> pathB = pathExcludingSource(b.source(), b.destination());

        Position bestCell = null;
        long bestTimeDiff = Long.MAX_VALUE;

        for (Position cell : pathA) {
            if (!pathB.contains(cell)) continue;

            long timeA = arrivalTimeAt(a, cell, cellDurationMs);
            long timeB = arrivalTimeAt(b, cell, cellDurationMs);
            long timeDiff = Math.abs(timeA - timeB);

            if (timeDiff < bestTimeDiff) {
                bestTimeDiff = timeDiff;
                bestCell = cell;
            }
        }
        return bestCell;
    }

    static long arrivalTimeAt(Motion motion, Position cell, long cellDurationMs) {
        int distance = chebyshevDistance(motion.source(), cell);
        return motion.startTime() + distance * cellDurationMs;
    }


    static boolean pathPassesThrough(Motion motion, Position cell) {
        return pathExcludingSource(motion.source(), motion.destination()).contains(cell);
    }

    static Position cellBeforeSharedCell(Motion motion, Position sharedCell) {
        int stepRow = Integer.compare(motion.destination().row(), motion.source().row());
        int stepCol = Integer.compare(motion.destination().col(), motion.source().col());
        return new Position(sharedCell.row() - stepRow, sharedCell.col() - stepCol);
    }

    private static List<Position> fullPathInclusive(Position from, Position to) {
        int stepRow = Integer.compare(to.row(), from.row());
        int stepCol = Integer.compare(to.col(), from.col());
        List<Position> path = new ArrayList<>();
        int row = from.row(), col = from.col();
        int guard = 0;
        while (row != to.row() || col != to.col()) {
            // רשת ביטחון: המסלול חייב להיות ישר או אלכסוני מושלם, אחרת
            // row/col לעולם לא יגיעו יחד ליעד והלולאה תרוץ עד אינסוף -
            // כלומר קיפאון מוחלט ובלתי הפיך של השרת. עדיף להתפוצץ ברעש.
            if (++guard > MAX_PATH_LENGTH) {
                throw new IllegalArgumentException(
                        "Non-linear path is not supported: " + from + " -> " + to);
            }
            path.add(new Position(row, col));
            row += stepRow; col += stepCol;
        }
        path.add(new Position(to.row(), to.col()));
        return path;
    }

    private static int chebyshevDistance(Position a, Position b) {
        return Math.max(Math.abs(a.row() - b.row()), Math.abs(a.col() - b.col()));
    }
}
