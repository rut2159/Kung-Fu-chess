package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.model.Piece;

public record ScoreChangedEvent(Piece.Color color, int newScore) implements GameEvent {
}
