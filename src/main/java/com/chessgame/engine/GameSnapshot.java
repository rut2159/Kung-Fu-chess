
package com.chessgame.engine;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;


import java.util.Collections;
import java.util.List;

public final class GameSnapshot {
    private final int width;
    private final int height;
    private final List<PieceView> pieces;
    private final Position selectedCell;
    private final boolean gameOver;
    private final Piece.Color winner;

    public GameSnapshot(int width, int height, List<PieceView> pieces, Position selectedCell,
                         boolean gameOver, Piece.Color winner) {
        this.width = width;
        this.height = height;
        this.pieces = Collections.unmodifiableList(pieces);
        this.selectedCell = selectedCell;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    public int width() { return width; }
    public int height() { return height; }
    public List<PieceView> pieces() { return pieces; }
    public Position selectedCell() { return selectedCell; }
    public boolean isGameOver() { return gameOver; }
    public Piece.Color winner() { return winner; }

    public static final class PieceView {
        private final String id;
        private final Piece.Color color;
        private final Piece.Kind kind;
        private final Position position;
        private final Piece.State state;
        private final double displayRow;
        private final double displayCol;
        private final double cooldownRemaining;

        public PieceView(String id, Piece.Color color, Piece.Kind kind, Position position, Piece.State state) {
            this(id, color, kind, position, state, position.row(), position.col(), 0.0);
        }

        public PieceView(String id, Piece.Color color, Piece.Kind kind, Position position, Piece.State state,
                          double displayRow, double displayCol) {
            this(id, color, kind, position, state, displayRow, displayCol, 0.0);
        }

        public PieceView(String id, Piece.Color color, Piece.Kind kind, Position position, Piece.State state,
                          double cooldownRemaining) {
            this(id, color, kind, position, state, position.row(), position.col(), cooldownRemaining);
        }

        private PieceView(String id, Piece.Color color, Piece.Kind kind, Position position, Piece.State state,
                           double displayRow, double displayCol, double cooldownRemaining) {
            this.id = id;
            this.color = color;
            this.kind = kind;
            this.position = position;
            this.state = state;
            this.displayRow = displayRow;
            this.displayCol = displayCol;
            this.cooldownRemaining = cooldownRemaining;
        }

        public String id() { return id; }
        public Piece.Color color() { return color; }
        public Piece.Kind kind() { return kind; }
        public Position position() { return position; }
        public Piece.State state() { return state; }

        public double displayRow() { return displayRow; }
        public double displayCol() { return displayCol; }

        public double cooldownRemaining() { return cooldownRemaining; }
    }
}