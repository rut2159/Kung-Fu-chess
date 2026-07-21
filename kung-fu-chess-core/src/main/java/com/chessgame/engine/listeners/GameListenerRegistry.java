package com.chessgame.engine.listeners;

import com.chessgame.engine.GameEngine;

import java.util.ArrayList;
import java.util.List;

public final class GameListenerRegistry {
    private final List<GameListener> listeners = new ArrayList<>();

    public void add(GameListener listener) {
        listeners.add(listener);
    }

    public void remove(GameListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(GameEngine engine) {
        for (GameListener listener : listeners) {
            listener.onGameStateChanged(engine);
        }
    }
}
