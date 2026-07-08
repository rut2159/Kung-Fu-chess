package com.chessgame.moves;

import com.chessgame.board.BoardPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MoveManager / מנהל מהלכים
 *
 * תפקיד: "בעלים" בלעדי של רשימת ה-ActiveMove-ים הפעילים. public כי
 * GameEngine (חבילת השורש) קורא לכל המתודות שלה. שימו לב ש-ActiveMove
 * עצמו (הטיפוס שחוזר מ-collectArrived ומתקבל ב-startMove) הוא public,
 * אבל השדות הפנימיים שלו לא - כך ש-GameEngine יכול "להעביר" ActiveMove
 * דרך המנהל בלי אף פעם לחטט בתוכו.
 */
public final class MoveManager {
    private final List<ActiveMove> ongoingMoves = new ArrayList<>();

    /** האם יש כלי בתנועה שיוצא מהתא הנתון. */
    public boolean isPieceMoving(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.fromRow == row && move.fromCol == col) {
                return true;
            }
        }
        return false;
    }

    /** האם משבצת היעד הנתונה כבר "שמורה" למהלך אחר שבדרך אליה. */
    public boolean isSquareReserved(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.toRow == row && move.toCol == col) {
                return true;
            }
        }
        return false;
    }

    /** האם מסלול תנועה חדש חוצה מסלול של מהלך אחר שכבר בתנועה. */
    public boolean isPathBlocked(int fromRow, int fromCol, int toRow, int toCol) {
        int deltaRow = Math.abs(toRow - fromRow);
        int deltaCol = Math.abs(toCol - fromCol);
        boolean isStraightLine = deltaRow == 0 || deltaCol == 0 || deltaRow == deltaCol;
        if (!isStraightLine) return false;

        List<int[]> newPath = BoardPath.fullPathInclusive(fromRow, fromCol, toRow, toCol);

        for (ActiveMove move : ongoingMoves) {
            List<int[]> activePath = BoardPath.fullPathInclusive(move.fromRow, move.fromCol, move.toRow, move.toCol);
            if (pathsOverlap(newPath, activePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean pathsOverlap(List<int[]> pathA, List<int[]> pathB) {
        for (int[] cellA : pathA) {
            for (int[] cellB : pathB) {
                if (cellA[0] == cellB[0] && cellA[1] == cellB[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    /** רושם מהלך חדש כ"פעיל". */
    public void startMove(ActiveMove move) {
        ongoingMoves.add(move);
    }

    /** אוסף ומוציא מהרשימה את כל המהלכים שהגיעו ליעדם עד לזמן הנתון. */
    public List<ActiveMove> collectArrived(long gameClock) {
        List<ActiveMove> arrived = new ArrayList<>();
        Iterator<ActiveMove> iterator = ongoingMoves.iterator();
        while (iterator.hasNext()) {
            ActiveMove move = iterator.next();
            if (move.hasArrived(gameClock)) {
                arrived.add(move);
                iterator.remove();
            }
        }
        return arrived;
    }
}
