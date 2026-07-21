package com.chessgame.logging;

import com.chessgame.bus.events.ScoreChangedEvent;

public final class ScoreChangedSubscriber {

    public void onScoreChanged(ScoreChangedEvent event) {
        System.err.printf(
                "[SCORE] %s now has %d points%n",
                event.color(),
                event.newScore()
        );
    }
}
