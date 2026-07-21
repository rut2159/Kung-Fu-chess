package com.chessgame.server.dto;

public record MoveCommand(int fromRow, int fromCol, int toRow, int toCol) {
}
