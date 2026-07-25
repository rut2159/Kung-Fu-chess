package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;

/**
 * Published once when a game session begins.
 * Subscribers: opening animation, welcome sound.
 */
public record GameStartedEvent() implements GameEvent {
}
