package com.chessgame;

import com.chessgame.audio.SoundSubscriber;
import com.chessgame.bus.events.GameOverEvent;
import com.chessgame.bus.events.MoveMadeEvent;
import com.chessgame.bus.events.MoveRejectedEvent;
import com.chessgame.engine.GameEngine;
import com.chessgame.input.BoardMapper;
import com.chessgame.input.Controller;
import com.chessgame.model.Board;
import com.chessgame.realtime.SpeedConfig;

/**
 * Wraps the core GameSession with everything the desktop client needs:
 * pixel-based click/drag input (Controller) and local sound playback.
 *
 * board/gameEngine/controller are exposed directly (delegating to the wrapped
 * core session) so desktop call sites read exactly like they did before the
 * module split - only the type name changed, from GameSession to DesktopGameSession.
 */
public final class DesktopGameSession {
    public final Board board;
    public final GameEngine gameEngine;
    public final Controller controller;

    public DesktopGameSession(Board board) {
        this(board, SpeedConfig.STANDARD);
    }

    public DesktopGameSession(Board board, SpeedConfig speedConfig) {
        GameSession session = new GameSession(board, speedConfig);
        this.board = session.board;
        this.gameEngine = session.gameEngine;
        this.controller = new Controller(new BoardMapper(board), gameEngine);

        wireSoundSubscriber();
    }

    private void wireSoundSubscriber() {
        SoundSubscriber soundSubscriber = new SoundSubscriber();
        gameEngine.eventBus().subscribe(MoveMadeEvent.class, soundSubscriber::onMoveMade);
        gameEngine.eventBus().subscribe(MoveRejectedEvent.class, soundSubscriber::onMoveRejected);
        gameEngine.eventBus().subscribe(GameOverEvent.class, soundSubscriber::onGameOver);
    }
}
