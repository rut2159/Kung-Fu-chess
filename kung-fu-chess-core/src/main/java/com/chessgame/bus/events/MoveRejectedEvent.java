package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.rules.MoveReason;

/**
 * Published whenever a requested move is rejected (illegal move, motion in progress,
 * game already over, etc). Subscribers: illegal-move sound, UI feedback.
 */
public record MoveRejectedEvent(MoveReason reason) implements GameEvent {
}
