import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameEngine {
    private final List<ActiveMove> ongoingMoves = new ArrayList<>();
    private final Board board;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private long gameClock = 0;
    private GameState currentState = GameState.INIT;
    private int expectedCols = -1;
    private boolean isGameOver = false;

    private static final String VALID_TOKEN_REGEX = "^(\\.|[wb][KQRBNP])$";

    public GameEngine() {
        this.board = new Board(this.ongoingMoves);
    }

    public void start() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException e) {
            // התעלמות משגיאות עבור סביבות בדיקה אוטומטיות
        }
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;

        if (line.equals("Board:")) {
            currentState = GameState.PARSING_BOARD;
        } else if (line.startsWith("Commands:")) {
            currentState = GameState.PARSING_COMMANDS;
        } else if (currentState == GameState.PARSING_BOARD) {
            handleBoardParsing(line);
        } else if (currentState == GameState.PARSING_COMMANDS) {
            handleCommandParsing(line);
        }
    }

    private void handleBoardParsing(String line) {
        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            if (!token.matches(VALID_TOKEN_REGEX)) {
                System.out.println("ERROR UNKNOWN_TOKEN");
                System.exit(0);
            }
        }
        if (expectedCols == -1) {
            expectedCols = tokens.length;
        } else if (tokens.length != expectedCols) {
            System.out.println("ERROR ROW_WIDTH_MISMATCH");
            System.exit(0);
        }
        board.addRow(tokens);
    }

    private void handleCommandParsing(String line) {
        if (line.startsWith("click")) {
            executeClickCommand(line);
        } else if (line.startsWith("wait")) {
            executeWaitCommand(line);
        } else if (line.equals("print board")) {
            board.print();
        }
    }

    private boolean isPieceMoving(int row, int col) {
        for (ActiveMove move : ongoingMoves) {
            if (move.fromRow == row && move.fromCol == col) {
                return true; 
            }
        }
        return false;
    }

    private boolean canInitiateMove(int fromRow, int fromCol, int toRow, int toCol, char pieceColor) {
        for (ActiveMove move : ongoingMoves) {
            if (move.toRow == toRow && move.toCol == toCol) {
                return false;
            }
            if (move.piece.charAt(0) != pieceColor) {
                return false;
            }
            if (pathsIntersect(fromRow, fromCol, toRow, toCol, move)) {
                return false;
            }
        }
        return true;
    }

    private boolean pathsIntersect(int fR1, int fC1, int tR1, int tC1, ActiveMove move) {
        List<int[]> path1 = getPathCoordinates(fR1, fC1, tR1, tC1);
        List<int[]> path2 = getPathCoordinates(move.fromRow, move.fromCol, move.toRow, move.toCol);
        for (int[] p1 : path1) {
            for (int[] p2 : path2) {
                if (p1[0] == p2[0] && p1[1] == p2[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<int[]> getPathCoordinates(int r1, int c1, int r2, int c2) {
        List<int[]> path = new ArrayList<>();
        int dr = Integer.compare(r2, r1);
        int dc = Integer.compare(c2, c1);
        int currR = r1, currC = c1;

        while (currR != r2 || currC != c2) {
            path.add(new int[]{currR, currC});
            currR += dr;
            currC += dc;
        }
        path.add(new int[]{r2, c2});
        return path;
    }

    private void executeClickCommand(String line) {
        if (isGameOver) {
            return;
        }

        String[] parts = line.split("\\s+");
        if (parts.length < 3) return;

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        if (x < 0 || y < 0) return;

        int col = x / 100;
        int row = y / 100;
        if (!board.isValidCell(row, col)) return;

        String clickedPiece = board.getPiece(row, col);

        if (selectedRow == -1 && selectedCol == -1) {
            if (!clickedPiece.equals(".") && !isPieceMoving(row, col)) {
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            String selectedPiece = board.getPiece(selectedRow, selectedCol);
            
            if (!clickedPiece.equals(".") && clickedPiece.charAt(0) == selectedPiece.charAt(0)) {
                if (!isPieceMoving(row, col)) {
                    selectedRow = row;
                    selectedCol = col;
                }
            } else {
                MoveValidator validator = new MoveValidator(board, selectedPiece, selectedRow, selectedCol, row, col);

                if (validator.isValid() && canInitiateMove(selectedRow, selectedCol, row, col, selectedPiece.charAt(0))) {
                    int distance = Math.max(Math.abs(row - selectedRow), Math.abs(col - selectedCol));
                    long arrivalTime = gameClock + (distance * 1000L);

                    ongoingMoves.add(new ActiveMove(selectedRow, selectedCol, row, col, selectedPiece, arrivalTime));
                    board.clearCellStatic(selectedRow, selectedCol);
                }
                
                selectedRow = -1;
                selectedCol = -1;
            }
        }
    }

    private void executeWaitCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return;

        int ms = Integer.parseInt(parts[1]);
        gameClock += ms;

        Iterator<ActiveMove> iterator = ongoingMoves.iterator();
        while (iterator.hasNext()) {
            ActiveMove move = iterator.next();
            if (gameClock >= move.arrivalTime) { 
                
                String target = board.getPiece(move.toRow, move.toCol);
                if (target.length() >= 2 && target.charAt(1) == 'K') {
                    isGameOver = true; 
                }
                
                // דרישה 3: מנגנון הכתרה (Promotion) - רגלי שהגיע לשורה האחרונה הופך למלכה
                String pieceToPlace = move.piece;
                if (pieceToPlace.length() >= 2 && pieceToPlace.charAt(1) == 'P') {
                    int lastRow = (pieceToPlace.charAt(0) == 'w') ? 0 : board.getRowsCount() - 1;
                    if (move.toRow == lastRow) {
                        pieceToPlace = "" + pieceToPlace.charAt(0) + "Q";
                    }
                }
                
                board.setPieceStatic(move.toRow, move.toCol, pieceToPlace);
                iterator.remove();
            }
        }
    }
}