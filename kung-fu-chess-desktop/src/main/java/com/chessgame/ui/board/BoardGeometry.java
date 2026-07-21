package com.chessgame.ui.board;

/**
 * גיאומטריית-הלוח בפיקסלים (מיקום/גודל) - חישוב טהור, בלי שום תלות
 * ב-Swing מעבר לגדלים המספריים שהיא מקבלת.
 */
record BoardGeometry(int boardSize, int offsetX, int offsetY, int cellSize) {

    private static final double MARGIN_PERCENT = 0.06;

    static BoardGeometry compute(int panelWidth, int panelHeight, int cols) {
        int availableSize = Math.min(panelWidth, panelHeight);
        int margin = (int) (availableSize * MARGIN_PERCENT);
        int rawBoardSize = availableSize - (2 * margin);

        int cellSize = Math.max(1, rawBoardSize / cols);
        int boardSize = cellSize * cols;

        int offsetX = (panelWidth - boardSize) / 2;
        int offsetY = (panelHeight - boardSize) / 2;

        return new BoardGeometry(boardSize, offsetX, offsetY, cellSize);
    }

    /** האם קואורדינטה יחסית-ללוח (אחרי הפחתת offset) בכלל בתוך גבולות הלוח. */
    boolean containsRelative(int relX, int relY) {
        return relX >= 0 && relY >= 0 && relX < boardSize && relY < boardSize;
    }
}
