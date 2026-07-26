package com.chessgame.bus;

import java.util.function.Consumer;

/**
 * A publish/subscribe bus for GameEvents.
 * Publishers don't know who (if anyone) is listening.
 * Subscribers only receive the specific event type they registered for.
 */
public interface EventBus {
    <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> subscriber);

    <T extends GameEvent> void unsubscribe(Class<T> eventType, Consumer<T> subscriber);

    <T extends GameEvent> void publish(T event);
}
