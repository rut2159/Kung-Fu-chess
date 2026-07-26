package com.chessgame.realtime.airborne;

import com.chessgame.model.Piece;
import com.chessgame.model.Position;

public record AirborneCapture(Piece defender, Piece victim, Position cell, long timestamp) {

    public boolean kingCaptured() {
        return victim.kind() == Piece.Kind.KING;
    }
}