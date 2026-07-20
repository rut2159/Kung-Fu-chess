package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.engine.moves.MoveRecord;

public record MoveMadeEvent(MoveRecord record) implements GameEvent {
}
