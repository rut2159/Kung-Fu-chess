package com.chessgame.bus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory EventBus implementation.
 *
 * Uses CopyOnWriteArrayList for the subscriber lists so that publish() can safely
 * iterate even if a subscriber subscribes/unsubscribes during handling (e.g. a
 * listener that removes itself after firing once).
 */
public final class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscribersByType = new ConcurrentHashMap<>();

    @Override
    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        subscribersByType
                .computeIfAbsent(eventType, type -> new CopyOnWriteArrayList<>())
                .add(subscriber);
    }

    @Override
    public <T extends GameEvent> void unsubscribe(Class<T> eventType, Consumer<T> subscriber) {
        List<Consumer<?>> subscribers = subscribersByType.get(eventType);
        if (subscribers != null) {
            subscribers.remove(subscriber);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        List<Consumer<?>> subscribers = subscribersByType.get(event.getClass());
        if (subscribers == null) {
            return;
        }
        for (Consumer<?> subscriber : subscribers) {
            try {
                ((Consumer<T>) subscriber).accept(event);
            } catch (RuntimeException e) {
                // A misbehaving subscriber must not prevent other subscribers from
                // running, and must not propagate back into the publisher's call stack.
                System.err.println("[EventBus] Subscriber threw for " + event.getClass().getSimpleName()
                        + ": " + e);
            }
        }
    }
}
