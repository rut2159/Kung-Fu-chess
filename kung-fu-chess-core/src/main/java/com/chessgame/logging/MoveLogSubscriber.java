package com.chessgame.logging;

import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.engine.moves.MoveRecord;

/**
 * Subscribes to MoveMadeEvent and writes a human-readable log line per move.
 * This class knows nothing about GameEngine, the bus internals, or how events
 * are delivered - it only knows what to do once a MoveMadeEvent arrives.
 * That's the point of Pub/Sub: this class is fully decoupled from the publisher.
 */
public final class MoveLogSubscriber {

    public void onMoveMade(MoveMadeEvent event) {
        MoveRecord record = event.record();
        System.err.printf(
                "[MOVE] %s %s: %s -> %s%s (t=%d)%n",
                record.color(),
                record.kind(),
                record.source(),
                record.destination(),
                record.isCapture() ? " (capture!)" : "",
                record.timestamp()
        );
    }
}
