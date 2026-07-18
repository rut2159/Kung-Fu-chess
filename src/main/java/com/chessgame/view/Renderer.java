package com.chessgame.view;

import com.chessgame.engine.GameSnapshot;
import com.chessgame.model.Piece;

public final class Renderer {

    public String draw(GameSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        appendBoardGrid(sb, snapshot);
        appendSelection(sb, snapshot);
        appendGameOverMessage(sb, snapshot);

        return sb.toString();
    }

    private void appendBoardGrid(StringBuilder sb, GameSnapshot snapshot) {
        String[][] grid = new String[snapshot.height()][snapshot.width()];
        for (String[] row : grid) {
            java.util.Arrays.fill(row, ".");
        }

        for (GameSnapshot.PieceView piece : snapshot.pieces()) {
            String marker = symbol(piece);
            if (piece.state() == Piece.State.AIRBORNE) {
                marker = marker.toLowerCase() + "^";
            }
            grid[piece.position().row()][piece.position().col()] = marker;
        }

        for (int row = 0; row < grid.length; row++) {
            sb.append(String.join(" ", grid[row]));
            sb.append("\n");
        }
    }

    private void appendSelection(StringBuilder sb, GameSnapshot snapshot) {
        if (snapshot.selectedCell() != null) {
            sb.append("selected: ").append(snapshot.selectedCell()).append("\n");
        }
    }

    private void appendGameOverMessage(StringBuilder sb, GameSnapshot snapshot) {
        if (snapshot.isGameOver()) {
            sb.append("GAME OVER\n");
        }
    }

    private String symbol(GameSnapshot.PieceView piece) {
        char color = piece.color() == Piece.Color.WHITE ? 'w' : 'b';
        char kind;
        switch (piece.kind()) {
            case KING:   kind = 'K'; break;
            case QUEEN:  kind = 'Q'; break;
            case ROOK:   kind = 'R'; break;
            case BISHOP: kind = 'B'; break;
            case KNIGHT: kind = 'N'; break;
            default:     kind = 'P';
        }
        return "" + color + kind;
    }
}
