package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;

/**
 * Published once when the game transitions to game-over state (e.g. a king was captured).
 * Subscribers: end-of-game animation, result sound, server-side game-end broadcast.
 *
 * Note: winner information isn't tracked yet in GameState - that's a good next
 * exercise once this event is wired in.
 */
public record GameOverEvent() implements GameEvent {
}
