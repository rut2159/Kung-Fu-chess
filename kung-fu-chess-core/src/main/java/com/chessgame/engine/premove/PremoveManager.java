package com.chessgame.engine.premove;

import com.chessgame.model.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PremoveManager {
    private final Map<Position, Position> pending = new HashMap<>();

    public void set(Position source, Position destination) {
        pending.put(source, destination);
    }

    public void clear(Position source) {
        pending.remove(source);
    }

    public Optional<Position> get(Position source) {
        return Optional.ofNullable(pending.get(source));
    }
}
