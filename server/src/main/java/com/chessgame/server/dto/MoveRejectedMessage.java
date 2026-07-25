package com.chessgame.server.dto;

public record MoveRejectedMessage(String username, String reason) {
}
