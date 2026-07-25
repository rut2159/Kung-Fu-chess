package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.model.Piece;

/**
 * Published after a move whose result changes a player's material score
 * (e.g. a capture). Subscribers: scoreboard UI, server-side score sync.
 */
public record ScoreChangedEvent(Piece.Color color, int newScore) implements GameEvent {
}
