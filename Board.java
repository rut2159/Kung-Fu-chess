import java.util.ArrayList;
import java.util.List;

public class Board {
    private final List<String[]> grid = new ArrayList<>();
    private final List<ActiveMove> ongoingMoves;

    public Board(List<ActiveMove> ongoingMoves) {
        this.ongoingMoves = ongoingMoves;
    }

    public void addRow(String[] row) {
        this.grid.add(row);
    }

    public int getRowsCount() {
        return grid.size();
    }

    public int getColsCount() {
        return grid.isEmpty() ? 0 : grid.get(0).length;
    }

    public String getPiece(int row, int col) {
        if (!isValidCell(row, col)) return ".";
        
        String staticPiece = grid.get(row)[col];
        if (!staticPiece.equals(".")) {
            return staticPiece;
        }
        
        for (ActiveMove move : ongoingMoves) {
            if (move.fromRow == row && move.fromCol == col) {
                return move.piece;
            }
        }
        return ".";
    }

    public void setPieceStatic(int row, int col, String piece) {
        if (isValidCell(row, col)) grid.get(row)[col] = piece;
    }

    public void clearCellStatic(int row, int col) {
        setPieceStatic(row, col, ".");
    }

    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < getRowsCount() && col >= 0 && col < getColsCount();
    }

    public void print() {
        for (int r = 0; r < getRowsCount(); r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < getColsCount(); c++) {
                sb.append(getPiece(r, c));
                if (c < getColsCount() - 1) {
                    sb.append(" ");
                }
            }
            System.out.println(sb.toString());
        }
    }
}