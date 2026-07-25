package com.chessgame.realtime;

import com.chessgame.model.Piece;
import com.chessgame.realtime.motion.Motion;

/**
 * What actually happened when a motion arrived - as opposed to what was
 * predicted/assumed when the move was first requested. A capture can only be
 * known for certain at arrival: the target may have moved away during the
 * attacker's travel time (real-time chess), so "was this a capture" must
 * never be decided any earlier than this.
 */
public record ArrivalOutcome(Motion motion, Piece.Kind movedKind, boolean captured, boolean kingCaptured) {
}
