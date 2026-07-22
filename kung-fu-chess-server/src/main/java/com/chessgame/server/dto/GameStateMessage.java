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
     * Translates a core GameSnapshot into the wire format sent to clients.
     * This is the one place that knows about both the core's shape and the
     * server's JSON shape - keeping that knowledge out of both core and DTO.
     *
     * whiteScore/blackScore aren't part of GameSnapshot itself (they come from
     * GameEngine.score(color) separately, same as desktop's PlayerPanelController
     * reads them) - so they're passed in rather than read off the snapshot.
     */
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
