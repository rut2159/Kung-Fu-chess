package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.rules.MoveReason;


public record MoveRejectedEvent(MoveReason reason) implements GameEvent {
}
