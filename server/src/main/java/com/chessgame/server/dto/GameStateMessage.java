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

    public static GameStateMessage from(GameSnapshot snapshot, String whiteUsername, String blackUsername,
                                         int whiteScore, int blackScore) {
        List<PieceDto> pieceDtos = snapshot.pieces().stream()
                .map(piece -> new PieceDto(
                        piece.id(),
                        piece.color().name(),
                        piece.kind().name(),
                        piece.position().row(),
                        piece.position().col(),
                        piece.displayRow(),
                        piece.displayCol(),
                        piece.state().name(),
                        piece.cooldownRemaining(),
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
