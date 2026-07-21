package com.chessgame.bus;

/**
 * Marker interface implemented by every event that can travel on the {@link EventBus}.
 * Each concrete event (e.g. MoveMadeEvent) carries only the data its subscribers need -
 * unlike GameListenerRegistry, which always hands the whole GameEngine to every listener.
 */
public interface GameEvent {
}
