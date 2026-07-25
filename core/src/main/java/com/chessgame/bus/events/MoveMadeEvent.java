package com.chessgame.bus.events;

import com.chessgame.bus.GameEvent;
import com.chessgame.engine.moves.MoveRecord;

/**
 * Published whenever a move is accepted and applied to the board.
 * Subscribers: move-log panel, server-side move logger, sound player.
 */
public record MoveMadeEvent(MoveRecord record) implements GameEvent {
}
