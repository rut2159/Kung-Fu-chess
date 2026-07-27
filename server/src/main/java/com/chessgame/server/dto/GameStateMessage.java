package com.chessgame.server.dto;

import com.chessgame.engine.snapshot.GameSnapshot;
import com.chessgame.model.Piece;

import java.util.List;

public record GameStateMessage(
        int width,
        int height,
        List<PieceDto> pieces,
        boolean gameOver,
        String winner,
        String whiteUsername,
        String blackUsername,
        int whiteScore,
        int blackScore,
        String disconnectedUsername,
        Integer resignInSeconds
) {

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    /**
     * @param forfeitWinner who takes the game when a player walked away. The
     *                      snapshot cannot know: both kings are still standing,
     *                      so it reports no winner. Ignored unless the game is
     *                      actually over, and always second to the board's own
     *                      verdict - a captured king outranks a forfeit.
     */
    public static GameStateMessage from(GameSnapshot snapshot, String whiteUsername, String blackUsername,
                                         int whiteScore, int blackScore,
                                         String disconnectedUsername, Integer resignInSeconds,
                                         Piece.Color forfeitWinner) {
        List<PieceDto> pieceDtos = snapshot.pieces().stream()
                .map(piece -> new PieceDto(
                        piece.id(),
                        piece.color().name(),
                        piece.kind().name(),
                        piece.position().row(),
                        piece.position().col(),
                        round(piece.displayRow()),
                        round(piece.displayCol()),
                        piece.state().name(),
                        round(piece.cooldownRemaining()),
                        piece.hasPremove()
                ))
                .toList();

        Piece.Color winner = snapshot.winner();
        if (winner == null && snapshot.isGameOver()) {
            winner = forfeitWinner;
        }
        String winnerName = winner != null ? winner.name() : null;

        return new GameStateMessage(
                snapshot.width(),
                snapshot.height(),
                pieceDtos,
                snapshot.isGameOver(),
                winnerName,
                whiteUsername,
                blackUsername,
                whiteScore,
                blackScore,
                disconnectedUsername,
                resignInSeconds
        );
    }
}
