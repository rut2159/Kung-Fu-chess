package com.chessgame.engine.moves;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoveHistory {
    private final List<MoveRecord> moves = new ArrayList<>();

    public void record(MoveRecord move) {
        moves.add(move);
    }

    public List<MoveRecord> all() {
        return Collections.unmodifiableList(new ArrayList<>(moves));
    }
}
