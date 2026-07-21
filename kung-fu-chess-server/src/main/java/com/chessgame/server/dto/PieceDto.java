package com.chessgame.server.dto;

/**
 * Wire-format representation of one piece, translated from the core's
 * GameSnapshot.PieceView. Deliberately flat and JSON-friendly - the client
 * doesn't need to know anything about the core's internal types.
 */
public record PieceDto(
        String id,
        String color,
        String kind,
        int row,
        int col,
        double displayRow,
        double displayCol,
        String state,
        double cooldownRemaining,
        boolean hasPremove
) {
}
