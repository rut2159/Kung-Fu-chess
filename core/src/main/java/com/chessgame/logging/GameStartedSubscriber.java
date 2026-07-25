package com.chessgame.logging;

import com.chessgame.bus.events.GameStartedEvent;

public final class GameStartedSubscriber {

    public void onGameStarted(GameStartedEvent event) {
        System.err.printf("GAME STARTED%n");
    }
}
