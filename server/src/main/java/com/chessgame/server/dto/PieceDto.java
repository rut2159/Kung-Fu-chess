package com.chessgame.server.dto;

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
