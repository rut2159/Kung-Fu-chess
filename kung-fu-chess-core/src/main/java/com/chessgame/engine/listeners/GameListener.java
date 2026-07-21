package com.chessgame.engine.listeners;

import com.chessgame.engine.GameEngine;

public interface GameListener {
    void onGameStateChanged(GameEngine engine);
}
