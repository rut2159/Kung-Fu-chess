package com.chessgame.server.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This is real-time chess: a move takes time to travel, and captures/cooldowns
 * only resolve when GameEngine.wait(ms) advances the game clock (see
 * RealTimeArbiter). A desktop client drives this from its render loop; a
 * server has no render loop, so a fixed scheduled tick plays that role here.
 */
@Component
public final class GameTicker {

    private static final String GAME_STATE_TOPIC = "/topic/game";
    private static final int TICK_MILLISECONDS = 50;

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameTicker(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = TICK_MILLISECONDS)
    public void tick() {
        gameService.advanceTime(TICK_MILLISECONDS);
        messagingTemplate.convertAndSend(GAME_STATE_TOPIC, gameService.currentState());
    }
}
