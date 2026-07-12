package com.chessgame.realtime.motion;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MotionManager {
    private final List<Motion> activeMotions = new ArrayList<>();

    /** האם יש תנועה פעילה שיוצאת מהתא הנתון. */
    public boolean isPieceMoving(Position position) {
        for (Motion m : activeMotions) {
            if (m.source().equals(position)) return true;
        }
        return false;
    }

    /** האם משבצת היעד כבר "שמורה" לתנועה אחרת שבדרך אליה. */
    public boolean isSquareReserved(Position destination) {
        for (Motion m : activeMotions) {
            if (m.destination().equals(destination)) return true;
        }
        return false;
    }

    /** האם מסלול תנועה חדש חוצה מסלול של תנועה קיימת. */
    public boolean isPathBlocked(Position source, Position destination) {
        List<Position> newPath = fullPathInclusive(source, destination);
        for (Motion existing : activeMotions) {
            List<Position> existingPath = fullPathInclusive(existing.source(), existing.destination());
            for (Position p : newPath) {
                if (existingPath.contains(p)) return true;
            }
        }
        return false;
    }

    /** רושם תנועה חדשה כ"פעילה", ומעדכן את מצב הכלי ל-MOVING. */
    public void startMove(Position source, Position destination, Piece piece, long arrivalTime) {
        piece.setState(Piece.State.MOVING);
        activeMotions.add(new Motion(source, destination, piece, arrivalTime));
    }

    /** אוסף ומוציא מהרשימה את כל התנועות שהגיעו ליעדן עד לזמן הנתון. */
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

    /**
     * נתיב מלא (כולל קצוות) בין שתי נקודות. משמעותי רק לתנועה
     * בקו-ישר/אלכסון (רוק/רץ/מלכה) - לתנועות אחרות (כמו L-shape
     * של פרש) אין "מסלול ביניים" אמיתי, אז מחזירים רק את שתי
     * הנקודות עצמן (וגם נמנעים מלולאה אינסופית).
     */
    private static List<Position> fullPathInclusive(Position from, Position to) {
        int deltaRow = to.row() - from.row();
        int deltaCol = to.col() - from.col();
        boolean straightOrDiagonal = deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol);

        List<Position> path = new ArrayList<>();
        if (!straightOrDiagonal) {
            path.add(from);
            path.add(to);
            return path;
        }

        int stepRow = Integer.compare(to.row(), from.row());
        int stepCol = Integer.compare(to.col(), from.col());
        int row = from.row(), col = from.col();
        while (row != to.row() || col != to.col()) {
            path.add(new Position(row, col));
            row += stepRow; col += stepCol;
        }
        path.add(new Position(to.row(), to.col()));
        return path;
    }
}
