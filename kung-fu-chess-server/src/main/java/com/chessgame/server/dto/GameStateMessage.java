package com.chessgame.server.dto;

import com.chessgame.engine.snapshot.GameSnapshot;

import java.util.List;

public record GameStateMessage(
        int width,
        int height,
        List<PieceDto> pieces,
        boolean gameOver,
        String winner
) {

    /**
     * Translates a core GameSnapshot into the wire format sent to clients.
     * This is the one place that knows about both the core's shape and the
     * server's JSON shape - keeping that knowledge out of both core and DTO.
     */
    public static GameStateMessage from(GameSnapshot snapshot) {
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
                winnerName
        );
    }
}
