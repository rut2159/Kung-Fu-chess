package com.chessgame;

import com.chessgame.board.Board;
import com.chessgame.board.BoardParser;
import com.chessgame.moves.ActiveMove;
import com.chessgame.moves.JumpManager;
import com.chessgame.moves.MoveArrivalProcessor;
import com.chessgame.moves.MoveManager;
import com.chessgame.moves.MoveValidator;
import com.chessgame.pieces.Piece;
import com.chessgame.rules.MoveContext;
import com.chessgame.rules.MoveRuleRegistry;
import com.chessgame.rules.standard.StandardChessRuleSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * GameEngine / מנוע המשחק
 *
 * תפקיד: מתאם דק בלבד. שימו לב לרשימת ה-import - זו בעצם "מפת
 * התלויות" של המנוע: הוא תלוי בכל אחת מארבע החבילות (pieces, board,
 * rules, moves), אבל אף אחת מהן לא תלויה בו בחזרה. זה בדיוק כיוון
 * התלות הנכון - השורש (המתאם) תלוי בשכבות, השכבות לא תלויות בשורש.
 */
class GameEngine {
    private final Board board = new Board();
    private final BoardParser boardParser = new BoardParser();
    private final MoveManager moveManager = new MoveManager();
    private final JumpManager jumpManager = new JumpManager();
    private final MoveRuleRegistry ruleRegistry = StandardChessRuleSet.build();
    private final MoveValidator moveValidator = new MoveValidator(ruleRegistry);
    private final MoveArrivalProcessor arrivalProcessor =
            new MoveArrivalProcessor(board, moveManager, jumpManager, ruleRegistry);

    private int selectedRow = -1;
    private int selectedCol = -1;
    private long gameClock = 0;
    private GameState currentState = GameState.INIT;
    private boolean gameOver = false;

    /**
     * נקודת ההתחלה של המנוע: קורא קלט מהמשתמש ומפעיל את הפקודות.
     */
    void start() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    /**
     * מנתב את הקלט לפי המצב הנוכחי של המשחק (קליטת לוח או פקודות).
     */
    private void processLine(String line) {
        if (line.isEmpty()) return;

        if (line.equals("Board:")) {
            currentState = GameState.PARSING_BOARD;
            return;
        } else if (line.startsWith("Commands:")) {
            currentState = GameState.PARSING_COMMANDS;
            return;
        }

        if (currentState == GameState.PARSING_BOARD) {
            handleBoardParsing(line);
        } else if (currentState == GameState.PARSING_COMMANDS) {
            handleCommandParsing(line);
        }
    }

    /**
     * מנתח שורה של הלוח. הולידציה עצמה בחבילת board (BoardParser) -
     * כאן רק מתפסים את החריגה ומתרגמים אותה להתנהגות הידועה.
     */
    private void handleBoardParsing(String line) {
        try {
            board.addRow(boardParser.parseRow(line));
        } catch (BoardParser.BoardParseException e) {
            System.out.println("ERROR " + e.getMessage());
            System.exit(0);
        }
    }

    /**
     * מנתב את הפקודות (click, wait, jump, print) לטיפול המתאים.
     */
    private void handleCommandParsing(String line) {
        if (line.startsWith("click")) {
            if (gameOver) return;
            executeClickCommand(line);
        } else if (line.startsWith("wait")) {
            if (gameOver) return;
            executeWaitCommand(line);
        } else if (line.startsWith("jump")) {
            if (gameOver) return;
            executeJumpCommand(line);
        } else if (line.equals("print board")) {
            board.print();
        }
    }

    // ------------------------------------------------------------------
    // click
    // ------------------------------------------------------------------

    private void executeClickCommand(String line) {
        CommandParser.parseCellCommand(line, board).ifPresent(cell -> handleClick(cell.row, cell.col));
    }

    private void handleClick(int row, int col) {
        Piece clickedPiece = board.getPiece(row, col);

        if (selectedRow == -1) {
            trySelectPiece(row, col, clickedPiece);
        } else {
            handleClickWithSelection(row, col, clickedPiece);
        }
    }

    private void trySelectPiece(int row, int col, Piece clickedPiece) {
        if (clickedPiece.isEmpty()) return;
        if (moveManager.isPieceMoving(row, col) || jumpManager.isPieceAirborne(row, col)) return;

        selectedRow = row;
        selectedCol = col;
    }

    private void handleClickWithSelection(int row, int col, Piece clickedPiece) {
        if (moveManager.isPieceMoving(row, col)) return;

        Piece selectedPiece = board.getPiece(selectedRow, selectedCol);

        if (!clickedPiece.isEmpty() && selectedPiece.isFriendlyTo(clickedPiece)) {
            reselect(row, col);
        } else {
            attemptMove(row, col, selectedPiece);
        }
    }

    private void reselect(int row, int col) {
        if (jumpManager.isPieceAirborne(row, col)) return;
        selectedRow = row;
        selectedCol = col;
    }

    private void attemptMove(int toRow, int toCol, Piece selectedPiece) {
        MoveContext ctx = new MoveContext(board, selectedPiece, selectedRow, selectedCol, toRow, toCol);

        boolean valid = moveValidator.isValid(ctx)
                && !moveManager.isPathBlocked(selectedRow, selectedCol, toRow, toCol)
                && !moveManager.isSquareReserved(toRow, toCol);

        if (valid) {
            startMove(toRow, toCol, selectedPiece);
        }
        clearSelection();
    }

    private void startMove(int toRow, int toCol, Piece piece) {
        int distance = Math.max(Math.abs(toRow - selectedRow), Math.abs(toCol - selectedCol));
        long moveDuration = distance * 1000L;
        long arrivalTime = gameClock + moveDuration;

        moveManager.startMove(new ActiveMove(selectedRow, selectedCol, toRow, toCol, piece, arrivalTime));
    }

    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
    }

    // ------------------------------------------------------------------
    // jump
    // ------------------------------------------------------------------

    private void executeJumpCommand(String line) {
        CommandParser.parseCellCommand(line, board).ifPresent(cell -> handleJump(cell.row, cell.col));
    }

    private void handleJump(int row, int col) {
        Piece piece = board.getPiece(row, col);
        if (piece.isEmpty()) return;
        if (moveManager.isPieceMoving(row, col)) return;
        if (jumpManager.isPieceAirborne(row, col)) return;

        if (selectedRow == row && selectedCol == col) {
            clearSelection();
        }

        jumpManager.startJump(row, col, piece, gameClock);
    }

    // ------------------------------------------------------------------
    // wait
    // ------------------------------------------------------------------

    private void executeWaitCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return;

        int ms;
        try {
            ms = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        gameClock += ms;

        if (arrivalProcessor.processArrivals(gameClock)) {
            gameOver = true;
        }
        jumpManager.landExpiredJumps(gameClock);
    }
}
