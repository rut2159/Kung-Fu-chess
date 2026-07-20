package com.chessgame.bus;

import java.util.function.Consumer;

/**
 * A publish/subscribe bus for GameEvents.
 * Publishers don't know who (if anyone) is listening.
 * Subscribers only receive the specific event type they registered for.
 */
public interface EventBus {

    /**
     * Registers a subscriber for a specific event type.
     * @param eventType the event class to listen for, e.g. MoveMadeEvent.class
     * @param subscriber called with the event instance whenever one is published
     * @param <T> the event type
     */
    <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> subscriber);

    /**
     * Removes a previously registered subscriber for a specific event type.
     */
    <T extends GameEvent> void unsubscribe(Class<T> eventType, Consumer<T> subscriber);

    /**
     * Publishes an event to every subscriber registered for its exact type.
     */
    <T extends GameEvent> void publish(T event);
}
