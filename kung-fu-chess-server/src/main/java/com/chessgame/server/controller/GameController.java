package com.chessgame.server.controller;

import com.chessgame.engine.moves.MoveResult;
import com.chessgame.server.dto.JumpCommand;
import com.chessgame.server.dto.MoveCommand;
import com.chessgame.server.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private static final String GAME_STATE_TOPIC = "/topic/game";

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/move")
    public void onMove(MoveCommand command) {
        MoveResult result = gameService.handleMove(command);
        if (result.isAccepted()) {
            broadcastState();
        }
    }

    /**
     * Mirrors the desktop's double-click "jump" - resets an idle piece's own
     * cooldown in place without moving it (see Controller.jump / GameEngine.requestJump).
     */
    @MessageMapping("/jump")
    public void onJump(JumpCommand command) {
        MoveResult result = gameService.handleJump(command);
        if (result.isAccepted()) {
            broadcastState();
        }
    }

    private void broadcastState() {
        messagingTemplate.convertAndSend(GAME_STATE_TOPIC, gameService.currentState());
    }
}
