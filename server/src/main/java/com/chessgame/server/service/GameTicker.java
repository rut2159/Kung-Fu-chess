package com.chessgame.server.service;

import com.chessgame.server.game.Matchmaker;
import com.chessgame.server.game.RoomRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class GameTicker {

    private static final int TICK_MILLISECONDS = 50;

    private final RoomRegistry roomRegistry;
    private final Matchmaker matchmaker;

    public GameTicker(RoomRegistry roomRegistry, Matchmaker matchmaker) {
        this.roomRegistry = roomRegistry;
        this.matchmaker = matchmaker;
    }

    @Scheduled(fixedRate = TICK_MILLISECONDS)
    public void tick() {
        roomRegistry.tickAll(TICK_MILLISECONDS);
        matchmaker.tick(TICK_MILLISECONDS);
    }
}
