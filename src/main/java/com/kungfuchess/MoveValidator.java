package com.kungfuchess;

public class MoveValidator {
    private final Board board;
    private final char pieceType;
    private final char pieceColor;
    private final int fromRow, fromCol, toRow, toCol;
    private final int deltaRow, deltaCol;

    public MoveValidator(Board board, String piece, int fromRow, int fromCol, int toRow, int toCol) {
        this.board = board;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.pieceType = (piece != null && piece.length() >= 2) ? piece.charAt(1) : '?';
        this.pieceColor = (piece != null && piece.length() >= 1) ? piece.charAt(0) : '?';
        this.deltaRow = Math.abs(toRow - fromRow);
        this.deltaCol = Math.abs(toCol - fromCol);
    }

    public boolean isValid() {
        if (fromRow == toRow && fromCol == toCol)
            return false;
        if (pieceType == '?')
            return false;

        String target = board.getPiece(toRow, toCol);
        if (!target.equals(".") && target.charAt(0) == pieceColor) {
            return false;
        }

        switch (pieceType) {
            case 'K':
                return deltaRow <= 1 && deltaCol <= 1;
            case 'R':
                return (deltaRow == 0 || deltaCol == 0) && isPathClear();
            case 'B':
                return (deltaRow == deltaCol) && isPathClear();
            case 'Q':
                return (deltaRow == deltaCol || deltaRow == 0 || deltaCol == 0) && isPathClear();
            case 'N':
                return (deltaRow == 1 && deltaCol == 2) || (deltaRow == 2 && deltaCol == 1);
            case 'P':
                return isValidPawnMove(target);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(String target) {
        int direction = (pieceColor == 'w') ? -1 : 1;
        int startRow = (pieceColor == 'w') ? board.getRowsCount() - 1 : 0;

        if (deltaCol == 0 && toRow == fromRow + direction) {
            return target.equals(".");
        }

        if (deltaCol == 0 && fromRow == startRow && toRow == fromRow + 2 * direction) {
            String intermediatePiece = board.getPiece(fromRow + direction, fromCol);
            return intermediatePiece.equals(".") && target.equals(".");
        }

        if (deltaCol == 1 && toRow == fromRow + direction) {
            return !target.equals(".");
        }

        return false;
    }

    private boolean isPathClear() {
        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currRow = fromRow + stepRow;
        int currCol = fromCol + stepCol;

        while (currRow != toRow || currCol != toCol) {
            if (!board.getPiece(currRow, currCol).equals(".")) {
                return false;
            }
            currRow += stepRow;
            currCol += stepCol;
        }
        return true;
    }
}
