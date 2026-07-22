package com.chessgame.server.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This is real-time chess: a move takes time to travel, and captures/cooldowns
 * only resolve when GameEngine.wait(ms) advances the game clock (see
 * RealTimeArbiter). A desktop client drives this from its render loop; a
 * server has no render loop, so a fixed scheduled tick plays that role here.
 *
 * The actual advance + broadcast happens inside GameService's single-threaded
 * queue (see advanceTimeAndBroadcast) - this class only decides *when*, not *how*.
 */
@Component
public final class GameTicker {

    private static final int TICK_MILLISECONDS = 50;

    private final GameService gameService;

    public GameTicker(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(fixedRate = TICK_MILLISECONDS)
    public void tick() {
        gameService.advanceTimeAndBroadcast(TICK_MILLISECONDS);
    }
}
