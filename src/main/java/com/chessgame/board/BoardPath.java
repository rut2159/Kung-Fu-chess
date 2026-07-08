package com.chessgame.board;

import java.util.ArrayList;
import java.util.List;

/**
 * BoardPath / נתיב לוח
 *
 * תפקיד: חישוב גיאומטרי טהור - "אילו תאים נמצאים בין נקודה A לנקודה B".
 * ממוקמת בחבילת board (ולא rules או moves) כי זו בעצם תכונה של טופולוגיית
 * הלוח עצמו - לא תלויה בכלים או בחוקי תנועה. public כי בשימוש הן
 * בחבילת rules (PathUtil) והן בחבילת moves (MoveManager).
 */
public final class BoardPath {
    private BoardPath() {
        // מחלקת עזר סטטית בלבד.
    }

    /**
     * נתיב מלא, כולל תא המוצא ותא היעד עצמם.
     * משמש את MoveManager לבדיקת חפיפה בין מסלולים מקבילים.
     */
    public static List<int[]> fullPathInclusive(int fromRow, int fromCol, int toRow, int toCol) {
        List<int[]> path = new ArrayList<>();
        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currRow = fromRow;
        int currCol = fromCol;
        while (currRow != toRow || currCol != toCol) {
            path.add(new int[]{currRow, currCol});
            currRow += stepRow;
            currCol += stepCol;
        }
        path.add(new int[]{toRow, toCol});
        return path;
    }

    /**
     * התאים שנמצאים ממש בין המוצא ליעד, לא כולל אף אחד מהם.
     * משמש את PathUtil לבדיקת "מסלול פנוי".
     */
    public static List<int[]> strictlyBetween(int fromRow, int fromCol, int toRow, int toCol) {
        List<int[]> path = new ArrayList<>();
        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currRow = fromRow + stepRow;
        int currCol = fromCol + stepCol;
        while (currRow != toRow || currCol != toCol) {
            path.add(new int[]{currRow, currCol});
            currRow += stepRow;
            currCol += stepCol;
        }
        return path;
    }
}
