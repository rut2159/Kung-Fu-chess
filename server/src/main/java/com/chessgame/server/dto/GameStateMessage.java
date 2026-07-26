package com.chessgame.server.dto;

import com.chessgame.engine.snapshot.GameSnapshot;

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
        int blackScore
) {

    /**
     * שלוש ספרות אחרי הנקודה זה כבר הרבה מתחת לגודל פיקסל על המסך, אבל
     * ההבדל בתעבורה גדול: double גולמי מסתדר כ-"5.123456789012345" - 17
     * תווים במקום 5, כפול שלושה שדות כפול 32 כלים, 20 פעמים בשנייה.
     */
    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static GameStateMessage from(GameSnapshot snapshot, String whiteUsername, String blackUsername,
                                         int whiteScore, int blackScore) {
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

        String winnerName = snapshot.winner() != null ? snapshot.winner().name() : null;

        return new GameStateMessage(
                snapshot.width(),
                snapshot.height(),
                pieceDtos,
                snapshot.isGameOver(),
                winnerName,
                whiteUsername,
                blackUsername,
                whiteScore,
                blackScore
        );
    }
}
