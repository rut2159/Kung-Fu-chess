package com.chessgame.logging;

import com.chessgame.bus.events.GameOverEvent;

public final class GameOverSubscriber {

    public void onGameOver(GameOverEvent event) {
        System.err.printf("GAME OVER%n");
    }
}
