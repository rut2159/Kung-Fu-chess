package com.chessgame.server.dto;


public record MoveHistoryEntryMessage(String color, String notation, String time, long timestampMs) {
}
