package com.chessgame.server.service;

import com.chessgame.server.game.Matchmaker;
import com.chessgame.server.game.RoomRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameTickerTest {

    private static final int TICK_MILLISECONDS = 50;

    @Mock
    private RoomRegistry roomRegistry;

    @Mock
    private Matchmaker matchmaker;

    @Test
    void tick_advancesBothRoomsAndMatchmakingByTheSameAmount() {
        GameTicker ticker = new GameTicker(roomRegistry, matchmaker);

        ticker.tick();

        verify(roomRegistry).tickAll(TICK_MILLISECONDS);
        verify(matchmaker).tick(TICK_MILLISECONDS);
    }
}
