package com.chessgame.server.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
